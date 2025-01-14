// This file is part of PDQ (https://github.com/ProofDrivenQuerying/pdq) which is released under the MIT license.
// See accompanying LICENSE for copyright notice and full details.

package uk.ac.ox.cs.pdq.cost.sqlserverhistogram;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

import uk.ac.ox.cs.pdq.algebra.AttributeEqualityCondition;
import uk.ac.ox.cs.pdq.algebra.ConstantEqualityCondition;
import uk.ac.ox.cs.pdq.cost.statistics.Histogram;

/**
 * Histograms for SQL Server 2014.
 * @author Efthymia Tsamoura
 *
 */
public class SQLServerHistogram implements Histogram{

	/** The buckets of the histogram. **/
	private final List<SQLServerBucket> buckets;
	
	
	/**
	 * Constructs histograms given the input buckets .
	 *
	 * @param buckets the buckets
	 */
	public SQLServerHistogram(List<SQLServerBucket> buckets) {
		Preconditions.checkArgument(buckets != null && !buckets.isEmpty() );
		this.buckets = buckets;
	}

	/**
	 * Gets the buckets.
	 *
	 * @return the buckets
	 */
	public List<SQLServerBucket> getBuckets() {
		return this.buckets;
	}
	
	/**
	 * Gets the bucket.
	 *
	 * @param bucketIndex the bucket index
	 * @return 		the buckets at the bucketIndex-th position of the histogram
	 */
	public SQLServerBucket getBucket(int bucketIndex) {
		Preconditions.checkArgument(this.buckets.size() > bucketIndex);
		return this.buckets.get(bucketIndex);
	}
	
	/**
	 * Gets the rows.
	 *
	 * @param predicate the predicate
	 * @return 		the number of database tuples that satisfy the input constant equality predicate
	 */
	public BigInteger getRows(ConstantEqualityCondition predicate) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	
	/**
	 * Gets the rows.
	 *
	 * @param predicate the predicate
	 * @return 		the number of database tuples that satisfy the input attribute equality predicate
	 */
	public BigInteger getRows(AttributeEqualityCondition predicate) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return Joiner.on("\n").join(this.buckets);
	}

	/**
	 * Equals.
	 *
	 * @param o Object
	 * @return boolean
	 */
	@Override
	public boolean equals(Object o) {
		return super.equals(o)
				&& this.getClass().isInstance(o)
				&& this.buckets.equals(((SQLServerHistogram) o).buckets)
				;

	}

	/**
	 * Hash code.
	 *
	 * @return int
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.buckets);
	}
	
}
