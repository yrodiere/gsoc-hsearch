/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.io.Serializable;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.search.jsr352.internal.JobContextData;
import org.jboss.logging.Logger;

@Named
public class PartitionAnalyzer implements javax.batch.api.partition.PartitionAnalyzer {

	private static final Logger logger = Logger.getLogger( PartitionAnalyzer.class );
	private final JobContext jobContext;
	private long workDone;

	@Inject
	public PartitionAnalyzer(JobContext jobContext) {
		this.jobContext = jobContext;
	}

	/**
	 * Analyze data obtained from different partition plans via partition data
	 * collectors. The current analyze is to summarize to their progresses :
	 * workCount = workCount1 + workCount2 + ... + workCountN Then it shows the
	 * total mass index progress in percentage. This method is very similar to
	 * the current simple progress monitor. Note: concerning the number of total
	 * entities loaded, it depends on 2 values : the number of row in the
	 * database table and the max results to process, defined by user before the
	 * job start. So the minimum between them will be used.
	 * 
	 * @param fromCollector the workCount obtained from partition collector's
	 * collectPartitionData
	 */
	@Override
	public void analyzeCollectorData(Serializable fromCollector)
			throws Exception {

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		long workTodo = jobData.getTotalEntityToIndex();
		workDone += (long) fromCollector;

		String percentStr = "??.?%";
		if (workTodo != 0) {
			percentStr = String.format( "%.1f%%", 100f * workDone / workTodo);
		}
		logger.infof( "%d works processed (%s).", workDone, percentStr );
	}

	@Override
	public void analyzeStatus(BatchStatus batchStatus, String exitStatus)
			throws Exception {
		logger.info( "analyzeStatus called." );
	}
}
