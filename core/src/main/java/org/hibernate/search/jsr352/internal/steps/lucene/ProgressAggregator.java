/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.io.Serializable;

import javax.batch.api.partition.AbstractPartitionAnalyzer;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.search.jsr352.internal.JobContextData;
import org.jboss.logging.Logger;

/**
 * Progress aggregator aggregates the intermediary chunk progress received from
 * each partition sent via the collectors. It runs on the step main thread.
 *
 * @author Mincong Huang
 */
@Named
public class ProgressAggregator extends AbstractPartitionAnalyzer {

	private static final Logger LOGGER = Logger.getLogger( ProgressAggregator.class );
	private final JobContext jobContext;

	@Inject
	public ProgressAggregator(JobContext jobContext) {
		this.jobContext = jobContext;
	}

	/**
	 * Analyze data obtained from different partition plans via partition data
	 * collectors. The current analyze is to summarize to their progresses :
	 * workDone = workDone1 + workDone2 + ... + workDoneN. Then it displays the
	 * total mass index progress in percentage. This method is very similar to
	 * the current simple progress monitor.
	 * 
	 * @param fromCollector the indexing progress of one partition, obtained
	 * from partition collector's method #collectPartitionData()
	 */
	@Override
	public void analyzeCollectorData(Serializable fromCollector) throws Exception {

		PartitionProgress partitionProgress = (PartitionProgress) fromCollector;

		// update step-level progress
		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		jobData.updateStepProgress( partitionProgress );

		StringBuilder sb = new StringBuilder( System.lineSeparator() );
		for ( String msg : jobData.getStepProgresses() ) {
			sb.append( System.lineSeparator() ).append( "\t" ).append( msg );
		}
		sb.append( System.lineSeparator() );
		LOGGER.info( sb.toString() );
	}
}
