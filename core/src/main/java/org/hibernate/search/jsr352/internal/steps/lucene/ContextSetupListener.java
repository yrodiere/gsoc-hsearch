/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.AbstractStepListener;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.internal.BatchContextData;
import org.hibernate.search.jsr352.internal.IndexingContext;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.store.IndexShardingStrategy;
import org.jboss.logging.Logger;

/**
 * Sets up the {@link EntityIndexingStepData} for one entity indexing step.
 *
 * @author Gunnar Morling
 * @author Mincong HUANG
 */
@Named
public class ContextSetupListener extends AbstractStepListener {

	private static final Logger LOGGER = Logger.getLogger( ContextSetupListener.class );

	private final JobContext jobContext;
	private final IndexingContext indexingContext;
	private final StepContext stepContext;

	@Inject @BatchProperty
	private String entityName;


	@Inject
	public ContextSetupListener(JobContext jobContext, StepContext stepContext,
			IndexingContext indexingContext) {
		this.jobContext = jobContext;
		this.indexingContext = indexingContext;
		this.stepContext = stepContext;
	}

	@Override
	public void beforeStep() throws Exception {
	}
}
