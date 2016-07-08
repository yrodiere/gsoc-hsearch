/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import javax.inject.Named;

import org.jboss.logging.Logger;

@Named
public class PartitionReducer implements javax.batch.api.partition.PartitionReducer {

	private static final Logger logger = Logger.getLogger( PartitionReducer.class );

	@Override
	public void beginPartitionedStep() throws Exception {
		logger.info( "#beginPartitionedStep() called." );
	}

	@Override
	public void beforePartitionedStepCompletion() throws Exception {
		logger.info( "#beforePartitionedStepCompletion() called." );
	}

	@Override
	public void rollbackPartitionedStep() throws Exception {
		logger.info( "#rollbackPartitionedStep() called." );
	}

	@Override
	public void afterPartitionedStepCompletion(PartitionStatus status)
			throws Exception {
		logger.info( "#afterPartitionedStepCompletion(...) called." );
	}

}
