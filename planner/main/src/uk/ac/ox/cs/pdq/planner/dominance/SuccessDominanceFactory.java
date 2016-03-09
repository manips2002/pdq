package uk.ac.ox.cs.pdq.planner.dominance;

import uk.ac.ox.cs.pdq.cost.estimators.AccessCountCostEstimator;
import uk.ac.ox.cs.pdq.cost.estimators.CostEstimator;
import uk.ac.ox.cs.pdq.cost.estimators.SimpleCostEstimator;
import uk.ac.ox.cs.pdq.plan.Plan;
import uk.ac.ox.cs.pdq.planner.PlannerParameters.SuccessDominanceTypes;
import uk.ac.ox.cs.pdq.util.Costable;

// TODO: Auto-generated Javadoc
/**
 * Creates success dominance detectors using the input parameters.
 * The available options are:
 * 		uk.ac.ox.cs.pdq.dominance.ClosedPlanCostDominance
 *
 * @author Efthymia Tsamoura
 * @param <P> the generic type
 */
public class SuccessDominanceFactory<P extends Costable> {

	/** The estimator. */
	private final CostEstimator<P> estimator;
	
	/** The type. */
	private final SuccessDominanceTypes type;

	/**
	 * Constructor for SuccessDominanceFactory.
	 * @param estimator CostEstimator<P>
	 * @param type SuccessDominanceTypes
	 */
	public SuccessDominanceFactory(CostEstimator<P> estimator, SuccessDominanceTypes type) {
		this.estimator = estimator;
		this.type = type;
	}

	/**
	 * Gets the single instance of SuccessDominanceFactory.
	 *
	 * @return SuccessDominance
	 */
	public SuccessDominance getInstance() {
		switch(this.type) {
		case CLOSED:
			return new ClosedSuccessDominance(this.estimator instanceof SimpleCostEstimator);
		case OPEN:
			SimpleCostEstimator<Plan> sc0 = new AccessCountCostEstimator<>();
			return new OpenSuccessDominance(this.estimator instanceof SimpleCostEstimator, sc0);
		default:
			return new ClosedSuccessDominance(this.estimator instanceof SimpleCostEstimator);
		}
	}
}