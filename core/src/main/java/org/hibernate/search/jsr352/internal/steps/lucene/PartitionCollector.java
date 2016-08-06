/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.io.Serializable;

import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.logging.Logger;

/**
 * @author Mincong Huang
 */
@Named
public class PartitionCollector implements javax.batch.api.partition.PartitionCollector {

	private static final Logger logger = Logger.getLogger( PartitionCollector.class );
	private StepContext stepContext;

	@Inject
	public PartitionCollector(StepContext stepContext) {
		this.stepContext = stepContext;
	}

	/**
	 * The collectPartitionData method receives control periodically during
	 * partition processing. This method receives control on each thread
	 * processing a partition as lucene document production, once at the end of
	 * the process.
	 */
	@Override
	public Serializable collectPartitionData() throws Exception {
		logger.info( "Collecting partition data ..." );
		return ( (StepContextData) stepContext.getTransientUserData() )
				.getPartitionProgress();
	}
}
