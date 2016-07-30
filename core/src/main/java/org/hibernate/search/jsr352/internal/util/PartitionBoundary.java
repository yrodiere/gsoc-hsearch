package org.hibernate.search.jsr352.internal.util;

/**
 * Partition boundary helps us to identify the lower boundary and upper boundary
 * of a given partition, with which we can define the two ends of the scrollable
 * results. All the attributes in this class are set to final, no possibility
 * to be modified after the construction.
 *
 * @author Mincong Huang
 */
public class PartitionBoundary {

	private final boolean isFirstPartition;
	private final boolean isLastPartition;
	private final Object upperID;
	private final Object lowerID;

	public PartitionBoundary(Object lowerID, Object upperID) {

		this.lowerID = lowerID;
		this.upperID = upperID;

		boolean isFirstPartition = false;
		boolean isLastPartition = false;

		if ( lowerID == null && upperID == null ) {
			String msg = "lowerID and upperID cannot be null at the same time.";
			throw new NullPointerException( msg );
		}
		if ( lowerID == null && upperID != null ) {
			isFirstPartition = true;
		}
		if ( lowerID != null && upperID == null ) {
			isLastPartition = true;
		}

		this.isFirstPartition = isFirstPartition;
		this.isLastPartition = isLastPartition;
	}

	public boolean isFirstPartition() {
		return isFirstPartition;
	}

	public boolean isLastPartition() {
		return isLastPartition;
	}

	public Object getUpperID() {
		return upperID;
	}

	public Object getLowerID() {
		return lowerID;
	}

	@Override
	public String toString() {
		return "PartitionBoundary [isFirstPartition=" + isFirstPartition
				+ ", isLastPartition=" + isLastPartition + ", upperID="
				+ upperID + ", lowerID=" + lowerID + "]";
	}
}
