package uk.ac.ox.cs.pdq.cost.statistics.estimators;

import uk.ac.ox.cs.pdq.algebra.Access;
import uk.ac.ox.cs.pdq.algebra.DependentAccess;
import uk.ac.ox.cs.pdq.algebra.DependentJoin;
import uk.ac.ox.cs.pdq.algebra.Distinct;
import uk.ac.ox.cs.pdq.algebra.Join;
import uk.ac.ox.cs.pdq.algebra.RelationalOperator;
import uk.ac.ox.cs.pdq.algebra.Scan;
import uk.ac.ox.cs.pdq.algebra.Selection;
import uk.ac.ox.cs.pdq.algebra.Union;
import uk.ac.ox.cs.pdq.algebra.predicates.ConjunctivePredicate;
import uk.ac.ox.cs.pdq.db.AccessMethod;
import uk.ac.ox.cs.pdq.db.AccessMethod.Types;
import uk.ac.ox.cs.pdq.db.Relation;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.plan.EstimateProvider;

/**
 * Compute the estimated input and output cardinalities of a logical operator
 * and its descendants, based on a naive criteria, in particular using fixed
 * selectivity ratios.
 *
 * @author Julien Leblay
 */
public class NaiveCardinalityEstimator extends AbstractCardinalityEstimator<NaiveMetadata> {

	public static final Double UNION_REDUCTION = 2.0;
	public static final Double SELECTIVITY_REDUCTION = 10.0;
	public static final Double DISTINCT_REDUCTION = 2.0;
	public static final Double JOIN_REDUCTION = 10.0;

	protected final Schema schema;

	/**
	 * Constructor for NaiveCardinalityEstimator.
	 * @param schema Schema
	 */
	public NaiveCardinalityEstimator(Schema schema) {
		this.schema = schema;
	}

	/**
	 * @return NaiveCardinalityEstimator
	 * @see uk.ac.ox.cs.pdq.cost.statistics.estimators.CardinalityEstimator#clone()
	 */
	@Override
	public NaiveCardinalityEstimator clone() {
		return new NaiveCardinalityEstimator(this.schema);
	}

	/**
	 * @param o LogicalOperator
	 * @return NaiveMetadata
	 */
	@Override
	protected NaiveMetadata initMetadata(RelationalOperator o) {
		return new NaiveMetadata();
	}

	/**
	 * @param o Scan
	 * @return Double
	 */
	@Override
	protected Double estimateOutput(Scan o) {
		return (double) this.schema.getRelation(o.getRelation().getName()).getMetadata().getSize();
	}

	/**
	 * @param o LogicalOperator
	 * @return Double
	 */
	private Double getParentInputCardinality(RelationalOperator o) {
		EstimateProvider<RelationalOperator> metadata = this.getMetadata(o);
		RelationalOperator parent = metadata.getParent();
		if (parent != null) {
			return this.getMetadata(parent).getInputCardinality();
		}
		return 0.0;
	}

	/**
	 * @param o Join
	 * @return Double
	 */
	@Override
	protected Double estimateOutput(Join o) {
		Double result = 1.0;
		Double largestChild = 1.0;
		Double inputCard = this.getParentInputCardinality(o);

		// Compute the horizontal increase of input card.
		Double rightInputCard = inputCard;
		RelationalOperator leftChild = o.getChildren().get(0);
		NaiveMetadata lcMetadata = this.getMetadata(leftChild);
		lcMetadata.setParent(o);
		lcMetadata.setInputCardinality(inputCard);
		this.estimateIfNeeded(leftChild);
		if (o instanceof DependentJoin) {
			rightInputCard = lcMetadata.getOutputCardinality() * Math.max(1.0, inputCard);
		}

		// Compute the join cardinality itself.
		RelationalOperator rightChild = o.getChildren().get(1);
		NaiveMetadata rcMetadata = this.getMetadata(rightChild);
		rcMetadata.setParent(o);
		rcMetadata.setInputCardinality(rightInputCard);
		this.estimateIfNeeded(rightChild);
		for (RelationalOperator child: o.getChildren()) {
			Double childCard = this.getMetadata(child).getOutputCardinality();
			result *= childCard;
			largestChild = Math.max(largestChild, childCard);
		}
		return Math.max(largestChild,
				result / Math.pow(JOIN_REDUCTION,
						((ConjunctivePredicate) o.getPredicate()).size()));
	}

	/**
	 * @param o Access
	 * @return Double
	 */
	@Override
	protected Double estimateOutput(Access o) {
		RelationalOperator child = o.getChild();
		if (child == null) {
			return (double) o.getRelation().getMetadata().getSize();
		}
		this.estimateIfNeeded(child);
		return (this.getMetadata(child).getOutputCardinality()
				* o.getRelation().getMetadata().getSize()
				/ SELECTIVITY_REDUCTION);
	}

	/**
	 * @param o DependentAccess
	 * @return Double
	 */
	@Override
	protected Double estimateOutput(DependentAccess o) {
		AccessMethod binding = o.getAccessMethod();
		Relation relation = o.getRelation();
		if (binding.getType() == Types.FREE) {
			return (double) relation.getMetadata().getSize();
		}
		return Math.max(1.0, (long) (relation.getMetadata().getSize()
				/ Math.pow(SELECTIVITY_REDUCTION, binding.getInputs().size())));
	}

	/**
	 * @param o Selection
	 * @return Double
	 */
	@Override
	protected Double estimateOutput(Selection o) {
		RelationalOperator child = o.getChild();
		NaiveMetadata cMetadata = this.getMetadata(child);
		cMetadata.setParent(o);
		Double inputCard = this.getParentInputCardinality(o);
		cMetadata.setInputCardinality(inputCard);
		this.estimate(child);
		return Math.max(1L,
				(cMetadata.getOutputCardinality()
						/ Math.pow(SELECTIVITY_REDUCTION,
								((ConjunctivePredicate) o.getPredicate()).size())));
	}

	/**
	 * @param o Distinct
	 * @return Double
	 */
	@Override
	protected Double estimateOutput(Distinct o) {
		RelationalOperator child = o.getChild();
		NaiveMetadata cMetadata = this.getMetadata(child);
		cMetadata.setParent(o);
		Double inputCard = this.getParentInputCardinality(o);
		cMetadata.setInputCardinality(inputCard);
		this.estimate(child);
		return (cMetadata.getOutputCardinality() / DISTINCT_REDUCTION);
	}

	/**
	 * @param o Union
	 * @return Double
	 */
	@Override
	protected Double estimateOutput(Union o) {
		Double result = 0.0;
		for (RelationalOperator child: o.getChildren()) {
			NaiveMetadata cMetadata = this.getMetadata(child);
			cMetadata.setParent(o);
			Double inputCard = this.getParentInputCardinality(o);
			cMetadata.setInputCardinality(inputCard);
			this.estimate(child);
			result += cMetadata.getOutputCardinality();
		}
		result /= UNION_REDUCTION;
		return result;
	}
}