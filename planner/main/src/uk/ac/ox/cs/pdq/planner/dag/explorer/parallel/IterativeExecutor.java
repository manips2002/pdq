package uk.ac.ox.cs.pdq.planner.dag.explorer.parallel;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import uk.ac.ox.cs.pdq.LimitReachedException;
import uk.ac.ox.cs.pdq.db.Constraint;
import uk.ac.ox.cs.pdq.fol.Query;
import uk.ac.ox.cs.pdq.planner.PlannerException;
import uk.ac.ox.cs.pdq.planner.dag.DAGChaseConfiguration;
import uk.ac.ox.cs.pdq.planner.dag.equivalence.DAGEquivalenceClasses;

// TODO: Auto-generated Javadoc
/**
 * Provides methods to create binary configurations or to
 * identify the minimum-cost configuration among a given set of configurations.
 *
 * @author Efthymia Tsamoura
 */
public abstract class IterativeExecutor {

	/** The context. */
	private final Context context;

	/**
	 * Constructor for IterativeExecutor.
	 * @param context Context
	 */
	public IterativeExecutor(Context context) {
		this.context = context;
	}

	/**
	 * Gets the context.
	 *
	 * @return Context
	 */
	public Context getContext() {
		return this.context;
	}
	
	/**
	 * Creates new binary configurations by combining configurations from the input left and right collections.
	 * If twoWay=TRUE the output configurations are of the form BinaryConfiguration(L,R) and BinaryConfiguration(R,L), where L belongs to the
	 * left input collection and R to the right input collection, respectively.
	 * Otherwise, they are of the form  BinaryConfiguration(L,R)
	 *
	 * @param depth 		The depth of the output configurations
	 * @param left 		The configurations to consider on the left
	 * @param right 		The configurations to consider on the right
	 * @param query the query
	 * @param dependencies the dependencies
	 * @param best 	 	The minimum cost closed and successful configuration found so far. The plans that correspond to the
	 * 		returned configurations have cost < the bestConfiguration
	 * @param classes the classes
	 * @param twoWay the two way
	 * @param timeout the timeout
	 * @param unit the unit
	 * @return Collection<DAGChaseConfiguration>
	 * @throws PlannerException the planner exception
	 * @throws LimitReachedException the limit reached exception
	 */
	public abstract Collection<DAGChaseConfiguration> reason(
			int depth,
			Queue<DAGChaseConfiguration> left,
			Collection<DAGChaseConfiguration> right,
			Query<?> query,
			Collection<? extends Constraint> dependencies,
			DAGChaseConfiguration best,
			DAGEquivalenceClasses classes, 
			boolean twoWay,
			long timeout, TimeUnit unit) throws PlannerException, LimitReachedException;

	/**
	 * Iterates over the input collection of configurations to identify the minimum-cost one.
	 *
	 * @param query the query
	 * @param input 		The input set of configurations
	 * @param classes 		Classes of structurally equivalent configurations
	 * @param best 		The minimum cost closed and successful configuration found so far. The plans that correspond to the
	 * 		returned configurations have cost < the bestConfiguration
	 * @param timeout the timeout
	 * @param unit the unit
	 * @return 		the non-dominated configurations (that could lead to the minimum-cost configuration),
	 * 		the minimum-cost configuration with cost < the cost of the input bestConfiguration and the successful configurations
	 * @throws PlannerException the planner exception
	 * @throws LimitReachedException the limit reached exception
	 */
	public abstract ExplorationResults explore(
			Query<?> query,
			Queue<DAGChaseConfiguration> input,
			DAGEquivalenceClasses classes,
			DAGChaseConfiguration best,
			long timeout, TimeUnit unit) throws PlannerException, LimitReachedException;
}