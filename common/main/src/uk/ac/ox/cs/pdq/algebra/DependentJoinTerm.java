package uk.ac.ox.cs.pdq.algebra;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;


/**
 * 
 * @author Efthymia Tsamoura
 *
 */
public class DependentJoinTerm extends RelationalTerm {
	protected static final long serialVersionUID = 3160309108592668317L;

	protected final RelationalTerm[] children = new RelationalTerm[2];

	/** The join conditions. */
	protected final Condition followupJoinConditions;
	
	/** Input positions for the right hand child**/
	protected final Map<Integer,Integer> positionsInRightChildThatAreBoundFromLeftChild;

	/**  Cashed string representation. */
	protected String toString = null;

	private DependentJoinTerm(RelationalTerm child1, RelationalTerm child2) {
		super(AlgebraUtilities.computeInputAttributesForDependentJoinTerm(child1, child2), AlgebraUtilities.computeOutputAttributes(child1, child2));
		Assert.assertNotNull(child1);
		Assert.assertNotNull(child2);
		Assert.assertTrue(CollectionUtils.containsAny(Arrays.asList(child1.getOutputAttributes()),Arrays.asList(child2.getInputAttributes())));
		this.children[0] = child1;
		this.children[1] = child2;
		this.positionsInRightChildThatAreBoundFromLeftChild = AlgebraUtilities.computePositionsInRightChildThatAreBoundFromLeftChild(child1, child2);
		this.followupJoinConditions = AlgebraUtilities.computeJoinConditions(this.children);
	}
	
	public Map<Integer,Integer> getPositionsInLeftChildThatAreInputToRightChild() {
		return this.positionsInRightChildThatAreBoundFromLeftChild;
	}
	
	public Condition getFollowupJoinConditions() {
		return this.followupJoinConditions;
	}

	@Override
	public String toString() {
		if(this.toString == null) {
			StringBuilder result = new StringBuilder();
			result.append("DependentJoin");
			result.append('{');
			result.append('[').append(this.followupJoinConditions).append(']');
			result.append(this.children[0].toString());
			result.append(',');
			result.append(this.children[1].toString());
			result.append('}');
			this.toString = result.toString();
		}
		return this.toString;
	}

	@Override
	public RelationalTerm[] getChildren() {
		return this.children.clone();
	}
	
    public static DependentJoinTerm create(RelationalTerm child1, RelationalTerm child2) {
        return Cache.dependentJoinTerm.retrieve(new DependentJoinTerm(child1, child2));
    }
    
	@Override
	public RelationalTerm getChild(int childIndex) {
		Assert.assertTrue(childIndex == 0 || childIndex == 1);
		return this.children[childIndex];
	}
	
	@Override
	public Integer getNumberOfChildren() {
		return this.children.length;
	}
}
