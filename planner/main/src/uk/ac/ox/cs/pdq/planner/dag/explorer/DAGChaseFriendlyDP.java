package uk.ac.ox.cs.pdq.planner.dag.explorer;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;

import uk.ac.ox.cs.pdq.cost.Cost;
import uk.ac.ox.cs.pdq.cost.estimators.CostEstimator;
import uk.ac.ox.cs.pdq.db.DatabaseConnection;
import uk.ac.ox.cs.pdq.fol.ConjunctiveQuery;
import uk.ac.ox.cs.pdq.planner.PlannerException;
import uk.ac.ox.cs.pdq.planner.PlannerParameters;
import uk.ac.ox.cs.pdq.planner.accessibleschema.AccessibleSchema;
import uk.ac.ox.cs.pdq.planner.dag.BinaryConfiguration;
import uk.ac.ox.cs.pdq.planner.dag.DAGChaseConfiguration;
import uk.ac.ox.cs.pdq.planner.dag.explorer.filters.Filter;
import uk.ac.ox.cs.pdq.planner.dag.explorer.validators.Validator;
import uk.ac.ox.cs.pdq.planner.dominance.Dominance;
import uk.ac.ox.cs.pdq.planner.dominance.SuccessDominance;
import uk.ac.ox.cs.pdq.reasoning.chase.Chaser;
import uk.ac.ox.cs.pdq.util.LimitReachedException;

// TODO: Auto-generated Javadoc
/**
 * The exploration proceeds similarly to the GenericExplorer.
 * First, it checks whether or not the configurations
 * to be composed could lead to the optimal solution prior to creating the corresponding binary configuration.
 * If yes, then it creates a new binary configuration which is only further considered
 * if it is not dominated by an existing configuration.
 *
 * @author Efthymia Tsamoura
 *
 */
public class DAGChaseFriendlyDP extends DAGGeneric {

	/**  Removes dominated configurations *. */
	private final Dominance[] dominance;
	
	/**
	 * Instantiates a new DAG chase friendly dp.
	 *
	 * @param eventBus the event bus
	 * @param collectStats the collect stats
	 * @param parameters the parameters
	 * @param query 		The input user query
	 * @param accessibleQuery 		The accessible counterpart of the user query
	 * @param schema 		The input schema
	 * @param accessibleSchema 		The accessible counterpart of the input schema
	 * @param chaser 		Saturates configurations using the chase algorithm
	 * @param detector 		Detects homomorphisms during chasing
	 * @param costEstimator 		Estimates the cost of a plan
	 * @param successDominance 		Removes success dominated configurations
	 * @param dominance 		Removes dominated configurations
	 * @param filter 		Filters out configurations at the end of each iteration
	 * @param validators the validators
	 * @param maxDepth 		The maximum depth to explore
	 * @param orderAware True if pair selection is order aware
	 * @throws PlannerException the planner exception
	 * @throws SQLException 
	 */
	public DAGChaseFriendlyDP(
			EventBus eventBus, 
			boolean collectStats,
			PlannerParameters parameters,
			ConjunctiveQuery query,
			ConjunctiveQuery accessibleQuery,
			AccessibleSchema accessibleSchema, 
			Chaser chaser,
			DatabaseConnection connection,
			CostEstimator costEstimator,
			SuccessDominance successDominance,
			Dominance[] dominance,
			Filter filter, 
			List<Validator> validators,
			int maxDepth) throws PlannerException, SQLException {
		super(eventBus, collectStats, parameters, query, accessibleQuery, accessibleSchema, chaser, connection, costEstimator,
				successDominance, filter, validators, maxDepth);
		Preconditions.checkNotNull(dominance);
		Preconditions.checkNotNull(successDominance);
		this.dominance = dominance;
	}

	/**
	 * Main loop.
	 *
	 * @return Collection<DAGChaseConfiguration>
	 * @throws PlannerException the planner exception
	 * @throws LimitReachedException the limit reached exception
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Collection<DAGChaseConfiguration> exploreAllConfigurationsUpToCurrentDepth() throws PlannerException, LimitReachedException {
		Map<Pair<DAGChaseConfiguration,DAGChaseConfiguration>,DAGChaseConfiguration> last = new HashMap<>();
		Pair<DAGChaseConfiguration, DAGChaseConfiguration> pair = null;
		while ((pair = this.selector.getNextPairOfConfigurationsToCompose(this.depth)) != null) {
			if(!last.containsKey(pair)) {
				BinaryConfiguration configuration = new BinaryConfiguration(
						pair.getLeft(),
						pair.getRight());
				Cost cost = this.costEstimator.cost(configuration.getPlan());
				configuration.setCost(cost);
				if (this.bestPlan == null || !this.successDominance.isDominated(configuration.getPlan(), configuration.getCost(), this.bestPlan, this.bestCost)) {
					configuration.reasonUntilTermination(this.chaser, this.accessibleQuery, this.accessibleSchema.getInferredAccessibilityAxioms());
					if (ExplorerUtils.isDominated(this.dominance, this.getRight(), configuration) == null
							&& ExplorerUtils.isDominated(this.dominance, last.values(), configuration) == null) {
						if (configuration.isClosed()
								&& configuration.isSuccessful(this.accessibleQuery)) {
							this.setBestPlan(configuration);
						} else {
							last.put(pair, configuration);
						}
					}
				}
			}
			if (this.checkLimitReached()) {
				this.forcedTermination = true;
				break;
			}
		}
		return last.values();
	}
}
