/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.io.Serializable;

import org.hibernate.Session;

/**
 * Container for data specific to the entity indexing batch step
 * `produceLuceneDoc`. There're 2 types of counter here : partitionWorkCount and
 * chunkWorkCount. Notice that the batch runtime maintain one container-clone
 * per partition. So counters are not shared with other threads / partitions.
 * 
 * @author Gunnar Morling
 * @author Mincong Huang
 */
public class StepContextData implements Serializable {

	private static final long serialVersionUID = 1961574468720628080L;

	/**
	 * chunkWorkCount is an elementary count, the counter per chunk. It records
	 * how many items have been written in the current chunk. This value is
	 * overwritten be the item writer at the end of each #writeItems, since one
	 * chunk is: N read-calls + N process-calls + 1 write-call.
	 */
	private long chunkWorkCount = 0;

	/**
	 * partitionWorkCount is a total count, the counter per partition, sum of
	 * all the chunkWorkCount.
	 */
	private long partitionWorkCount = 0;

	/**
	 * Hibernate session, unwrapped from EntityManager. It is stored for sharing
	 * the session between item reader and item processor. Notice that item
	 * reader and item processor of the same partition always run in the same
	 * thread, so it should be OK. When the job stops, session object will be
	 * released before persisting this class's instance.
	 */
	private Session session;

	public long getChunkWorkCount() {
		return chunkWorkCount;
	}

	public void setChunkWorkCount(long increment) {
		this.chunkWorkCount = increment;
		this.partitionWorkCount += increment;
	}

	public void setPartitionWorkCount(long total) {
		this.partitionWorkCount = total;
	}

	public long getPartitionWorkCount() {
		return partitionWorkCount;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}
}
