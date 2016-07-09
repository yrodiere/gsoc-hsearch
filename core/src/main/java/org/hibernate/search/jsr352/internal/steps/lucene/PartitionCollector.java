/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.io.Serializable;

import javax.inject.Named;

@Named
public class PartitionCollector implements javax.batch.api.partition.PartitionCollector {

	/**
	 * The collectPartitionData method receives control periodically during
	 * partition processing. This method receives control on each thread
	 * processing a partition as IdProducerBatchlet, once at the end of the
	 * process.
	 */
	@Override
	public Serializable collectPartitionData() throws Exception {

		// get transient user data
		// LuceneData userData = (LuceneData) stepContext.getTransientUserData();

		// TODO Is this really required?
		// once data collected, reset the counter
		// to zero in transient user data
		// stepContext.setTransientUserData(0);

		// return userData.getProcessedWorkCount();
		// TODO: change mechanism
		return 0;
	}
}
