/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.io.Serializable;

/**
 * Container for data specific to the entity indexing batch step. There's 2
 * types of counter here : stepWorkCount and chunkWorkCount. Notice that the
 * batch runtime maintain one container-clone per partition. So the counters
 * are not shared with other threads / partitions.
 * <p>
 * <li>chunkWorkCount is the counter per chunk. One chunk is composed by N read
 * calls + N process calls + 1 write call.</li>
 * <li>partitionWorkCount is the counter per partition, sum of all the
 * chunkWorkCount.</li>
 *
 * @author Gunnar Morling
 * @author Mincong HUANG
 */
public class StepContextData implements Serializable {

	private static final long serialVersionUID = 1961574468720628080L;

	private long chunkWorkCount = 0;
	private long partitionWorkCount = 0; // sum of chunkWorkCount

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
}
