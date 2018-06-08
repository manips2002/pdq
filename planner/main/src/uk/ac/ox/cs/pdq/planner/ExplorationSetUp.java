package uk.ac.ox.cs.pdq.planner;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.google.common.eventbus.EventBus;

import uk.ac.ox.cs.pdq.algebra.AccessTerm;
import uk.ac.ox.cs.pdq.algebra.CartesianProductTerm;
import uk.ac.ox.cs.pdq.algebra.Condition;
import uk.ac.ox.cs.pdq.algebra.ConjunctiveCondition;
import uk.ac.ox.cs.pdq.algebra.ConstantEqualityCondition;
import uk.ac.ox.cs.pdq.algebra.DependentJoinTerm;
import uk.ac.ox.cs.pdq.algebra.JoinTerm;
import uk.ac.ox.cs.pdq.algebra.ProjectionTerm;
import uk.ac.ox.cs.pdq.algebra.RelationalTerm;
import uk.ac.ox.cs.pdq.algebra.RenameTerm;
import uk.ac.ox.cs.pdq.algebra.SelectionTerm;
import uk.ac.ox.cs.pdq.algebra.SimpleCondition;
import uk.ac.ox.cs.pdq.cost.Cost;
import uk.ac.ox.cs.pdq.cost.CostEstimatorFactory;
import uk.ac.ox.cs.pdq.cost.CostParameters;
import uk.ac.ox.cs.pdq.cost.estimators.CostEstimator;
import uk.ac.ox.cs.pdq.cost.logging.CostStatKeys;
import uk.ac.ox.cs.pdq.databasemanagement.DatabaseManager;
import uk.ac.ox.cs.pdq.databasemanagement.DatabaseParameters;
import uk.ac.ox.cs.pdq.databasemanagement.ExternalDatabaseManager;
import uk.ac.ox.cs.pdq.databasemanagement.InternalDatabaseManager;
import uk.ac.ox.cs.pdq.databasemanagement.LogicalDatabaseInstance;
import uk.ac.ox.cs.pdq.databasemanagement.cache.MultiInstanceFactCache;
import uk.ac.ox.cs.pdq.databasemanagement.exception.DatabaseException;
import uk.ac.ox.cs.pdq.databasemanagement.monitor.DatabaseMonitor;
import uk.ac.ox.cs.pdq.db.AccessMethodDescriptor;
import uk.ac.ox.cs.pdq.db.Attribute;
import uk.ac.ox.cs.pdq.db.Relation;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Conjunction;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.fol.Constant;
import uk.ac.ox.cs.pdq.fol.Dependency;
import uk.ac.ox.cs.pdq.fol.Formula;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.TypedConstant;
import uk.ac.ox.cs.pdq.fol.UntypedConstant;
import uk.ac.ox.cs.pdq.fol.Variable;
import uk.ac.ox.cs.pdq.logging.ChainedStatistics;
import uk.ac.ox.cs.pdq.logging.DynamicStatistics;
import uk.ac.ox.cs.pdq.logging.StatKey;
import uk.ac.ox.cs.pdq.planner.accessibleschema.AccessibleSchema;
import uk.ac.ox.cs.pdq.planner.logging.performance.ConstantsStatistics;
import uk.ac.ox.cs.pdq.planner.logging.performance.EventDrivenExplorerStatistics;
import uk.ac.ox.cs.pdq.planner.logging.performance.PlannerStatKeys;
import uk.ac.ox.cs.pdq.planner.util.PlannerUtility;
import uk.ac.ox.cs.pdq.reasoning.ReasonerFactory;
import uk.ac.ox.cs.pdq.reasoning.ReasoningParameters;
import uk.ac.ox.cs.pdq.reasoning.chase.ChaseConstantGenerator;
import uk.ac.ox.cs.pdq.reasoning.chase.Chaser;
import uk.ac.ox.cs.pdq.util.EventHandler;
import uk.ac.ox.cs.pdq.util.GlobalCounterProvider;

/**
 * Main entry point for chasing, where all properties are gathered and used to
 * create various objects like explorer,reasoner,cost estimator, etc...
 * <li>- implements an easy to use search function that has a query as input and
 * returns plans with costs for the given query over the previously configured
 * schema.</li><br>
 * <li>- Also creates and manages an eventBus.</li><br>
 * <li>- converts query into AccessibleQuery and maintains a map of its
 * variables to chase-constants.</li><br>
 * 
 * @author Julien Leblay
 * @author Efthymia Tsamoura
 * @author George Konstantinidis
 * @author Gabor
 *
 */
public class ExplorationSetUp {

	/** The log. */
	protected static Logger log = Logger.getLogger(ExplorationSetUp.class);

	/** Input parameters. */
	private PlannerParameters plannerParams;

	/** Input parameters. */
	private CostParameters costParams;

	/**  */
	private ReasoningParameters reasoningParams;

	/**  */
	private DatabaseParameters databaseParams;

	/**   */
	private EventBus eventBus = new EventBus();

	/** Statistics collector. */
	private ChainedStatistics statsLogger;

	/** The input schema with the original attribute types.  */
	private Schema originalSchema;
	/** Same as the original schema but the attribute types are converted to String. */
	private Schema schema;

	/**
	 * For each query it stores a Map of variables to chase constants.
	 */
	private static Map<ConjunctiveQuery, Map<Variable, Constant>> canonicalSubstitution = new HashMap<>();
	/**
	 * Same as above but it contains substitution for free variables only.
	 */
	private static Map<ConjunctiveQuery, Map<Variable, Constant>> canonicalSubstitutionOfFreeVariables = new HashMap<>();

	/** The external cost estimator. */
	private CostEstimator externalCostEstimator = null;

	/** The auxiliary schema, including axioms capturing access methods */
	private AccessibleSchema accessibleSchema;

	/**
	 * Instantiates a new exploration set up.
	 *
	 * @param planParams
	 *            the plan params
	 * @param costParams
	 *            the cost params
	 * @param reasoningParams
	 *            the reasoning params
	 * @param schema
	 *            the schema
	 */
	public ExplorationSetUp(PlannerParameters planParams, CostParameters costParams, ReasoningParameters reasoningParams, DatabaseParameters dbParams, Schema schema) {
		this(planParams, costParams, reasoningParams, dbParams, schema, null);
	}

	/**
	 * Instantiates a new exploration set up.
	 *
	 * @param params
	 *            the params
	 * @param costParams
	 *            the cost params
	 * @param reasoningParams
	 *            the reasoning params
	 * @param schema
	 *            the schema
	 * @param statsLogger
	 *            the stats logger
	 */
	public ExplorationSetUp(PlannerParameters params, CostParameters costParams, ReasoningParameters reasoningParams, DatabaseParameters databaseParams, Schema schema,
			ChainedStatistics statsLogger) {
		this.plannerParams = params;
		this.costParams = costParams;
		this.reasoningParams = reasoningParams;
		this.databaseParams = databaseParams;
		this.schema = convertTypesToString(schema);
		this.originalSchema = schema;
		//this.schema = schema;
		this.statsLogger = statsLogger;
		this.accessibleSchema = new AccessibleSchema(this.schema);
	}

	/**
	 * Register event handler.
	 *
	 * @param handler
	 *            EventHandler
	 */
	public void registerEventHandler(EventHandler handler) {
		this.eventBus.register(handler);
	}

	/**
	 * Register the given event homoChecker.
	 *
	 * @param handler
	 *            EventHandler
	 */
	public void unregisterEventHandler(EventHandler handler) {
		this.eventBus.unregister(handler);
	}

	/**
	 * Sets the cost estimator.
	 *
	 * @param estimator
	 *            CostEstimator<?>
	 */
	public void setCostEstimator(CostEstimator estimator) {
		this.externalCostEstimator = estimator;
	}

	/**
	 * Search a best plan for the given schema and query.
	 *
	 * @param <P>
	 *            the generic type
	 * @param query
	 *            the query
	 * @return a pair whose first element is the best plan found if any, null
	 *         otherwise, and the second is a mapping from the variables of the
	 *         input query to the constant generated in the initial grounded
	 *         operation.
	 * @throws PlannerException
	 *             the planner exception
	 * @throws SQLException
	 */
	public Entry<RelationalTerm, Cost> search(ConjunctiveQuery query) throws PlannerException, SQLException {
		return this.search(query, false);
	}

	/**
	 * Search for a best plan for the given schema and query.
	 *
	 * @param <S>
	 *            the generic type
	 * @param <P>
	 *            the generic type
	 * @param query
	 *            the query
	 * @param noDep
	 *            if true, dependencies in the schema are disabled and planning
	 *            occur taking only into account access-based axioms.
	 * @return a pair whose first element is the best plan found if any, null
	 *         otherwise, and the second is a mapping from the variables of the
	 *         input query to the constant generated in the initial grounded
	 *         operation.
	 * @throws PlannerException
	 *             the planner exception
	 * @throws SQLException
	 */
	public Entry<RelationalTerm, Cost> search(ConjunctiveQuery query, boolean noDep) throws PlannerException, SQLException {
		query = convertQueryConstantsToString(query);
		boolean collectStats = this.statsLogger != null;
		if (noDep) {
			this.schema = new Schema(this.schema.getRelations());
			this.accessibleSchema = new AccessibleSchema(this.schema);
		}
		ConjunctiveQuery accessibleQuery = generateAccessibleQueryAndStoreSubstitutionToCanonicalVariables(query);

		Explorer explorer = null;
		DatabaseManager databaseConnection;
		try {
			if (plannerParams.getUseInternalDatabase()) {
				// internal
				databaseConnection = new InternalDatabaseManager(new MultiInstanceFactCache(),
						GlobalCounterProvider.getNext("DatabaseInstanceId"));
			} else {
				// external database.
				if (this.databaseParams.getUseInternalDatabaseManager()) {
					databaseConnection = new InternalDatabaseManager(new MultiInstanceFactCache(),GlobalCounterProvider.getNext("DatabaseInstanceId"));
				} else {
					databaseConnection = new LogicalDatabaseInstance(new MultiInstanceFactCache(),
						new ExternalDatabaseManager(this.databaseParams),GlobalCounterProvider.getNext("DatabaseInstanceId"));
				}
			}
			
			databaseConnection.initialiseDatabaseForSchema(this.accessibleSchema);
		} catch (DatabaseException e1) {
			throw new PlannerException("Faild to create database",e1);
		}

		try {
			// Top-level initialisations
			CostEstimator costEstimator = this.externalCostEstimator;
			if (costEstimator == null)
				costEstimator = CostEstimatorFactory.getEstimator(this.costParams, this.schema);

			Chaser reasoner = new ReasonerFactory(this.eventBus, collectStats, this.reasoningParams).getInstance();

			explorer = ExplorerFactory.createExplorer(
					this.eventBus, collectStats, this.schema, this.accessibleSchema, query, accessibleQuery, 
					reasoner, databaseConnection,
					costEstimator, this.plannerParams, this.reasoningParams);

			// Chain all statistics collectors
			if (collectStats) {
				// Explorer statistics
				EventDrivenExplorerStatistics es = new EventDrivenExplorerStatistics();
				this.registerEventHandler(es);
				this.statsLogger.addStatistics(es);

				// Constant statistics
				ConstantsStatistics cs = new ConstantsStatistics();
				this.registerEventHandler(cs);
				this.statsLogger.addStatistics(cs);

				// Append dynamic statistics to logs
				DynamicStatistics ds = new DynamicStatistics(ExplorationSetUp.displayColumns());
				this.registerEventHandler(ds);
				this.statsLogger.addStatistics(ds);
			}
			explorer.setExceptionOnLimit(this.plannerParams.getExceptionOnLimit());
			explorer.setMaxRounds(this.plannerParams.getMaxIterations().doubleValue());
			explorer.setMaxElapsedTime(120l*1000l);
		//explorer.setMaxElapsedTime(this.plannerParams.getTimeout());
			explorer.explore();
			if (explorer.getBestPlan() != null && explorer.getBestCost() != null)
				return new AbstractMap.SimpleEntry<RelationalTerm, Cost>(convertTypesBack(explorer.getBestPlan()), explorer.getBestCost());
			else
				return null;
		} catch (PlannerException e) {
			this.handleEarlyTermination(explorer);
			throw e;
		} catch (UnsupportedOperationException  e) {
			if (e.getMessage()!=null && e.getMessage().contains("BLACKBOX_DB cost estimator is not currently supported.")) {
				// ignored exception case.
			} else {
				this.handleEarlyTermination(explorer);
				log.error(e.getMessage(), e);
			}
			throw new PlannerException(e);
		} catch (Exception e) {
			this.handleEarlyTermination(explorer);
			log.error(e.getMessage(), e);
			throw new PlannerException(e);
		} catch (Throwable e) {
			this.handleEarlyTermination(explorer);
			throw e;
		} finally {
			try {
				if (databaseConnection!=null) {
					databaseConnection.dropDatabase();
					databaseConnection.shutdown();
				}
				// cost estimators can have database managers started, and many others could do the same, kill them all.
				DatabaseMonitor.forceStopAll();
			} catch (Exception e) {
				this.handleEarlyTermination(explorer);
				e.printStackTrace();
			}
		}
	}

	/** Converts the output plan's attribute types and constant types back to the original schema's types.
	 * @param bestPlan
	 * @return
	 */
	private RelationalTerm convertTypesBack(RelationalTerm term) {
		if (term instanceof AccessTerm) {
			Relation relation = originalSchema.getRelation(((AccessTerm) term).getRelation().getName());
			AccessMethodDescriptor accessMethod = relation.getAccessMethod(((AccessTerm) term).getAccessMethod().getName());
			Map<Integer, TypedConstant> inputConstants = ((AccessTerm) term).getInputConstants();
			if ( inputConstants == null || inputConstants.isEmpty()) {
				AccessTerm newAccessTerm = AccessTerm.create(relation , accessMethod) ;
				return newAccessTerm;
			} else {
				Map<Integer, TypedConstant> convertedInputConstants = new HashMap<>();
				for (Integer index:inputConstants.keySet()) {
					TypedConstant newConstant = convertConstant(inputConstants.get(index), relation.getAttribute(index).getType());
					convertedInputConstants.put(index, newConstant);
				}
				AccessTerm newAccessTerm = AccessTerm.create(relation , accessMethod,convertedInputConstants) ;
				return newAccessTerm;
			}
		} 
		RelationalTerm child0 = convertTypesBack(term.getChild(0));
		RelationalTerm child1 = null;
		if (term.getChildren().length > 1) {
			child1 = convertTypesBack(term.getChild(1));
		}
		if (term instanceof RenameTerm) {
			RenameTerm rt = (RenameTerm)term;
			Attribute[] renamings = new Attribute[rt.getRenamings().length];
			for (int index = 0; index < rt.getRenamings().length; index++) {
				Attribute old = rt.getRenamings()[index];
				renamings[index] = Attribute.create(child0.getOutputAttribute(index).getType(), old.getName());
			}
			return RenameTerm.create(renamings , child0);
		}
		if (term instanceof SelectionTerm) {
			SelectionTerm st = (SelectionTerm)term;
			SimpleCondition[] sp = ((ConjunctiveCondition)st.getSelectionCondition()).getSimpleConditions();
			SimpleCondition[] spNew = new SimpleCondition[sp.length];
			for (int index = 0; index < sp.length; index++) {
				ConstantEqualityCondition old = (ConstantEqualityCondition) sp[index];
				spNew[index] = ConstantEqualityCondition.create(old.getPosition(), convertConstant(old.getConstant(),child0.getOutputAttributes()[old.getPosition()].getType()));
			}
			Condition predicate = ConjunctiveCondition.create(spNew);
			return SelectionTerm.create(predicate  , child0);
		}
		if (term instanceof JoinTerm) {
			return JoinTerm.create(child0, child1);
		}
		if (term instanceof DependentJoinTerm) {
			return JoinTerm.create(child0, child1);
		}
		if (term instanceof CartesianProductTerm) {
			return CartesianProductTerm.create(child0, child1);
		}
		if (term instanceof ProjectionTerm) {
			Attribute[] oldProjections = ((ProjectionTerm) term).getProjections();
			Attribute[] newProjections = new Attribute[oldProjections.length];
			for (int i = 0; i < oldProjections.length; i++) {
				for (Attribute a:child0.getOutputAttributes()) {
					if (a.getName().equals(oldProjections[i].getName())) {
						newProjections[i] = a;
					}
				}
			}
			return ProjectionTerm.create(newProjections, child0);
		}
		throw new UnsupportedOperationException("Can't convert " + term + ".");
	}

	private TypedConstant convertConstant(TypedConstant typedConstant, Type type) {
		if (typedConstant.getValue() != null && typedConstant.getValue() instanceof String)
			return TypedConstant.create(TypedConstant.convertStringToType((String)typedConstant.getValue(), type));
		else throw new UnsupportedOperationException("Can't convert " + typedConstant + " to type: " + type);
	}

	/** Converts all constants of the query to strings
	 * @param query
	 * @return
	 */
	private ConjunctiveQuery convertQueryConstantsToString(ConjunctiveQuery query) {
		
		Formula newAtom = convertQueryAtomConstantToString(query.getBody());
		if (newAtom instanceof Atom) {
			return ConjunctiveQuery.create(query.getFreeVariables(), new Atom[] {(Atom)newAtom});
		} else {
			return ConjunctiveQuery.create(query.getFreeVariables(), ((Conjunction)newAtom).getAtoms());
		}
	}

	/** converts all constants to strings.
	 * @param body
	 * @return
	 */
	private Formula convertQueryAtomConstantToString(Formula body) {
		if (body instanceof Atom) { 
			Term terms[] = body.getTerms();
			for (int i = 0; i < terms.length; i++) {
				if (terms[i] instanceof TypedConstant) {
					terms[i] = TypedConstant.create("" + ((TypedConstant)terms[i]).value);
				}
			}
			return Atom.create(((Atom) body).getPredicate(), terms);
		} else {
			Formula left = ((Conjunction)body).getChildren()[0];
			Formula right = ((Conjunction)body).getChildren()[1];
			return Conjunction.create(convertQueryAtomConstantToString(left),convertQueryAtomConstantToString(right));
		}
	}

	private Schema convertTypesToString(Schema schema) {
		List<Dependency> dep = new ArrayList<>();
		dep.addAll(Arrays.asList(schema.getNonEgdDependencies()));
		dep.addAll(Arrays.asList(schema.getKeyDependencies()));
		Relation[] rels = schema.getRelations();
		for (int i = 0; i < rels.length; i++) {
			rels[i] = createDatabaseRelation(rels[i]);
		}
		return new Schema(rels,dep.toArray(new Dependency[dep.size()]));
	}
	/**
	 * Creates the db relation. Currently codes in the position numbers into the
	 * names, but this should change
	 *
	 * @param relation
	 *            the relation
	 * @return a new database relation with attributes x0,x1,...,x_{N-1}, Fact where
	 *         x_i maps to the i-th relation's attribute
	 */
	private Relation createDatabaseRelation(Relation relation) {
		Attribute[] attributes = new Attribute[relation.getArity()];
		for (int index = 0; index < relation.getArity(); index++) {
			Attribute attribute = relation.getAttribute(index);
			attributes[index] = Attribute.create(String.class, attribute.getName());
		}
		return Relation.create(relation.getName(), attributes, relation.getAccessMethods(), relation.isEquality());
	}

	/**
	 * Display columns.
	 *
	 * @return StatKeys[]
	 */
	private static StatKey[] displayColumns() {
		return new StatKey[] { CostStatKeys.COST_ESTIMATION_COUNT, CostStatKeys.COST_ESTIMATION_TIME, PlannerStatKeys.GENERATED_FACTS, PlannerStatKeys.CANDIDATES,
				PlannerStatKeys.CUMULATED_CANDIDATES, PlannerStatKeys.EQUIVALENCE_CLASSES, PlannerStatKeys.AVG_EQUIVALENCE_CLASSES, PlannerStatKeys.MED_EQUIVALENCE_CLASSES,
				PlannerStatKeys.FILTERED, PlannerStatKeys.MILLI_REASONING, PlannerStatKeys.MILLI_UPDATE, PlannerStatKeys.MILLI_UPDATE_QUERY_DEPENDENCIES,
				PlannerStatKeys.MILLI_BLOCKING_CHECK, PlannerStatKeys.MILLI_SELECT_IC, PlannerStatKeys.MILLI_DETECT_CANDIDATES, PlannerStatKeys.MILLI_CLOSE,
				PlannerStatKeys.MILLI_QUERY_MATCH, PlannerStatKeys.MILLI_DOMINANCE, PlannerStatKeys.DOMINANCE_PRUNING, PlannerStatKeys.MILLI_EQUIVALENCE,
				PlannerStatKeys.EQUIVALENCE_PRUNING, PlannerStatKeys.HIGHER_COST_PRUNING };
	}

	/**
	 * Handle early termination.
	 *
	 * @param ex
	 *            Explorer<?>
	 */
	private void handleEarlyTermination(Explorer ex) {
		if (ex != null) {
			ex.updateClock();
			if (this.eventBus != null) {
				this.eventBus.post(ex);
			}
		}
	}

	/**
	 * Generate canonical mapping.
	 *
	 * @param formula
	 *            the body
	 * @return a mapping of variables of the input conjunction to constants. A fresh
	 *         constant is created for each variable of the conjunction. This method
	 *         is invoked by the conjunctive query constructor when the constructor
	 *         is called with empty input canonical mapping.
	 */
	public static ConjunctiveQuery generateAccessibleQueryAndStoreSubstitutionToCanonicalVariables(ConjunctiveQuery query) {
		Map<Variable, Constant> canonicalMapping = new LinkedHashMap<>();
		for (Atom atom : query.getBody().getAtoms()) {
			for (Term t : atom.getTerms()) {
				if (t.isVariable()) {
					Constant c = canonicalMapping.get(t);
					if (c == null) {
						c = UntypedConstant.create(ChaseConstantGenerator.getName());
						canonicalMapping.put((Variable) t, c);
					}
				}
			}
		}
		Map<Variable, Constant> substitutionFiltered = new HashMap<>();
		substitutionFiltered.putAll(canonicalMapping);
		for (Variable variable : query.getBoundVariables())
			substitutionFiltered.remove(variable);
		canonicalSubstitution.put(query, canonicalMapping);
		canonicalSubstitutionOfFreeVariables.put(query, substitutionFiltered);
		ConjunctiveQuery accessibleQuery = PlannerUtility.createAccessibleQuery(query);
		canonicalSubstitution.put(accessibleQuery, canonicalMapping);
		canonicalSubstitutionOfFreeVariables.put(accessibleQuery, substitutionFiltered);
		return accessibleQuery;
	}

	public static Map<ConjunctiveQuery, Map<Variable, Constant>> getCanonicalSubstitution() {
		return canonicalSubstitution;
	}

	public static Map<ConjunctiveQuery, Map<Variable, Constant>> getCanonicalSubstitutionOfFreeVariables() {
		return canonicalSubstitutionOfFreeVariables;
	}
}
