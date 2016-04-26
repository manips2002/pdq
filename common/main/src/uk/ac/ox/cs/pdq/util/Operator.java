package uk.ac.ox.cs.pdq.util;

import java.util.List;

import uk.ac.ox.cs.pdq.fol.Term;


// TODO: Auto-generated Javadoc
/**
 * Top-level interface for any operator supporting input and output types.
 * It can be a fined-grained operator such as a relational algebra operator,
 * or more coarse views on specific plan operators.
 * 
 * @author Julien Leblay
 */
public interface Operator extends Costable {

	/**
	 * Specifies how the output of this operator is sorted.
	 */
	public static enum SortOrder { 
 /** The asc. */
 ASC, 
 /** The desc. */
 DESC, 
 /** The unsorted. */
 UNSORTED }
	

	/**
 * Gets the input type.
 *
 * @return TupleType
 */
	TupleType getInputType();

	/**
	 * Gets the type.
	 *
	 * @return Type
	 */
	TupleType getType();

	/**
	 * Gets the columns.
	 *
	 * @return TupleType
	 */
	List<Term> getColumns();
}
