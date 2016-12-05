/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Step level progress. It contains the indexing progress of the step level. In
 * another word, it is the sum of all the elementary, partition-local level
 * progress. The progress is initialized at the first start of the indexing job
 * and reused after the restart.
 *
 * @author Mincong Huang
 */
public class StepProgress implements Serializable {

	private static final long serialVersionUID = 7808926033388850340L;
//	private Map<Integer, Long> partitionProgress;
//	private Map<Integer, Long> partitionTotal;

	/**
	 * A map of the total number of rows having already been indexed across all
	 * the entity types. Key: the entity name in string; Value: the number of
	 * rows indexed.
	 */
	private Map<String, Long> entityProgress;

	/**
	 * A map of the total number of rows to index across all the entity types.
	 * Key: the entity name in string; Value: the number of rows to index.
	 */
	private Map<String, Long> entityTotal;

	public StepProgress() {
//		partitionProgress = new HashMap<>();
//		partitionTotal = new HashMap<>();
		entityProgress = new HashMap<>();
		entityTotal = new HashMap<>();
	}

	public void increment(String entityName, int pid, long increment) {
		increment( entityName, increment );
//		increment( pid, increment );
	}

	private void increment(String entityName, long increment) {
		long prevDone = entityProgress.getOrDefault( entityName, 0L );
		entityProgress.put( entityName, prevDone + increment );
	}

//	private void increment(int pid, long increment) {
//		long prevDone = partitionProgress.getOrDefault( pid, 0L );
//		partitionProgress.put( pid, prevDone + increment );
//	}

//	/**
//	 * Get the progress of a given partition ID.
//	 * 
//	 * @param pid partition ID
//	 * @return a progress value varies between {@literal [0.0, 1.0]}.
//	 */
//	public double getProgress(int pid) {
//		if ( !partitionProgress.containsKey( pid )
//				|| !partitionTotal.containsKey( pid ) ) {
//			throw new NullPointerException( "PartitionId=" + pid + " not found." );
//		}
//		return partitionProgress.get( pid ) * 1.0 / partitionTotal.get( pid );
//	}

	/**
	 * Get the progress of a given entity.
	 * 
	 * @param entityName the name of entity
	 * @return a progress value varies between {@literal [0.0, 1.0]}.
	 */
	public double getProgress(String entityName) {
		if ( !entityProgress.containsKey( entityName )
				|| !entityTotal.containsKey( entityName ) ) {
			throw new NullPointerException( "entityName=" + entityName + " not found." );
		}
		return entityProgress.get( entityName ) * 1.0 / entityTotal.get( entityName );
	}

	public long getRowsToIndex(String entityName) {
		return entityTotal.get( entityName );
	}

	public void setRowsToIndex(String entityName, long rowsToIndex) {
		entityTotal.put( entityName, rowsToIndex );
	}
}
