package uk.ac.ox.cs.pdq.test.planner.dag.explorer;

import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import uk.ac.ox.cs.pdq.algebra.RelationalTerm;
import uk.ac.ox.cs.pdq.cost.Cost;
import uk.ac.ox.cs.pdq.cost.DoubleCost;
import uk.ac.ox.cs.pdq.cost.estimators.CostEstimator;
import uk.ac.ox.cs.pdq.db.AccessMethod;
import uk.ac.ox.cs.pdq.db.Attribute;
import uk.ac.ox.cs.pdq.db.DatabaseConnection;
import uk.ac.ox.cs.pdq.db.DatabaseParameters;
import uk.ac.ox.cs.pdq.db.Relation;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.fol.Atom;
import uk.ac.ox.cs.pdq.fol.Conjunction;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.fol.Term;
import uk.ac.ox.cs.pdq.fol.Variable;
import uk.ac.ox.cs.pdq.planner.PlannerException;
import uk.ac.ox.cs.pdq.planner.PlannerParameters;
import uk.ac.ox.cs.pdq.planner.PlannerParameters.FollowUpHandling;
import uk.ac.ox.cs.pdq.planner.accessibleschema.AccessibleSchema;
import uk.ac.ox.cs.pdq.planner.dag.explorer.DAGGeneric;
import uk.ac.ox.cs.pdq.planner.dag.explorer.validators.DefaultValidator;
import uk.ac.ox.cs.pdq.planner.dag.explorer.validators.Validator;
import uk.ac.ox.cs.pdq.planner.dominance.SuccessDominance;
import uk.ac.ox.cs.pdq.planner.util.PlannerUtility;
import uk.ac.ox.cs.pdq.reasoning.chase.RestrictedChaser;
import uk.ac.ox.cs.pdq.util.GlobalCounterProvider;
import uk.ac.ox.cs.pdq.util.LimitReachedException;

/**
 * 
 * @author Efthymia Tsamoura
 *
 */
public class TestDAGGeneric {

	protected Attribute a = Attribute.create(Integer.class, "a");
	protected Attribute b = Attribute.create(Integer.class, "b");
	protected Attribute c = Attribute.create(Integer.class, "c");
	protected Attribute d = Attribute.create(Integer.class, "d");
	protected Attribute InstanceID = Attribute.create(Integer.class, "InstanceID");
	
	@Test 
	public void test1() {
		GlobalCounterProvider.resetCounters();
		GlobalCounterProvider.getNext("CannonicalName");
		test1ExplorationSteps();
	}
	
	public void test1ExplorationSteps() {
		//Create the relations
		Relation[] relations = new Relation[5];
		relations[0] = Relation.create("R0", new Attribute[]{this.a, this.b, this.c, this.d, this.InstanceID}, 
				new AccessMethod[]{AccessMethod.create(new Integer[]{})});
		relations[1] = Relation.create("R1", new Attribute[]{this.a, this.b, this.c, this.d, this.InstanceID}, 
				new AccessMethod[]{AccessMethod.create(new Integer[]{2,3})});
		
		relations[2] = Relation.create("R2", new Attribute[]{this.a, this.b, this.c, this.d, this.InstanceID}, 
				new AccessMethod[]{AccessMethod.create(new Integer[]{})});
		relations[3] = Relation.create("R3", new Attribute[]{this.a, this.b, this.c, this.d, this.InstanceID}, 
				new AccessMethod[]{AccessMethod.create(new Integer[]{2,3})});
		relations[4] = Relation.create("Accessible", new Attribute[]{this.a,this.InstanceID});
		//Create query
		//R0(x,y,z,w) R1(_,_,z,w) R2(x,y,z',w') R3(_,_,z',w')
		Atom[] atoms = new Atom[4];
		Variable x = Variable.create("x");
		Variable y = Variable.create("y");
		Variable z = Variable.create("z");
		Variable w = Variable.create("w");
		atoms[0] = Atom.create(relations[0], new Term[]{x,y,z,w});
		atoms[1] = Atom.create(relations[1], new Term[]{Variable.create("x2"), Variable.create("y2"),z,w});
		atoms[2] = Atom.create(relations[2], new Term[]{x,y,Variable.create("z3"), Variable.create("w3")});
		atoms[3] = Atom.create(relations[3], new Term[]{Variable.create("x4"), Variable.create("y4"),Variable.create("z3"), Variable.create("w3")});
		ConjunctiveQuery query = ConjunctiveQuery.create(new Variable[]{x,y}, (Conjunction) Conjunction.of(atoms));
		
		//Create schema
		Schema schema = new Schema(relations);
		
		//Create accessible schema
		AccessibleSchema accessibleSchema = new AccessibleSchema(schema);
		
		//TODO assert that the accessible schema is fine
		
		//Create accessible query
		ConjunctiveQuery accessibleQuery = PlannerUtility.createAccessibleQuery(query, query.getSubstitutionOfFreeVariablesToCanonicalConstants());
	
		//Create database connection
		DatabaseConnection connection = null;
		try {
			connection = new DatabaseConnection(new DatabaseParameters(), accessibleSchema);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		//Create the chaser 
		RestrictedChaser chaser = new RestrictedChaser(null);
				
		//Mock the planner parameters
		PlannerParameters parameters = Mockito.mock(PlannerParameters.class);
		when(parameters.getSeed()).thenReturn(1);
		when(parameters.getFollowUpHandling()).thenReturn(FollowUpHandling.MINIMAL);
		
		//Mock the cost estimator 
		CostEstimator costEstimator = Mockito.mock(CostEstimator.class);
		when(costEstimator.cost(Mockito.any(RelationalTerm.class))).thenReturn(new DoubleCost(1.0));
		
		SuccessDominance successDominance = Mockito.mock(SuccessDominance.class);
		when(successDominance.isDominated(Mockito.any(RelationalTerm.class), Mockito.any(Cost.class), Mockito.any(RelationalTerm.class), Mockito.any(Cost.class))).thenReturn(false);
		
		List<Validator> validators = new ArrayList<>(); 
		validators.add(new DefaultValidator());
		
		try {
			DAGGeneric explorer = new DAGGeneric(null, false, parameters, query, accessibleQuery, accessibleSchema, chaser, connection, costEstimator, successDominance, null, validators, 4);
			explorer.explore();
			explorer.getExploredPlans();
			//TODO assert that we got all possible plans
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PlannerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LimitReachedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}