/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.io.Serializable;
import java.util.List;

import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.impl.StreamingOperationExecutor;
import org.hibernate.search.backend.impl.StreamingOperationExecutorSelector;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.internal.BatchContextData;
import org.hibernate.search.jsr352.internal.EntityIndexingStepData;
import org.hibernate.search.jsr352.internal.IndexingContext;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.store.IndexShardingStrategy;
import org.jboss.logging.Logger;

/**
 * Batch item writer writes a list of items into Lucene documents. Here, items
 * mean the luceneWorks, given by the processor. These items will be executed
 * using StreamingOperationExecutor.
 * <p>
 * <ul>
 * <li>{@code indexingContext} is used to store the shardingStrategy
 * <li>{@code monitor} mass indexer progress monitor helps to follow the mass
 * indexing progress and show it in the console.
 * </ul>
 *
 * @author Mincong HUANG
 */
@Named
public class ItemWriter implements javax.batch.api.chunk.ItemWriter {

	private final Boolean forceAsync = true;

	// TODO: The monitor is not used for instance. It should be used later.
	private MassIndexerProgressMonitor monitor;

	private JobContext jobContext;
	private StepContext stepContext;
	private IndexingContext indexingContext;

	@Inject
	@BatchProperty
	private String entityType;

	@Inject
	public ItemWriter(JobContext jobContext,
			StepContext stepContext,
			IndexingContext indexingContext) {
		this.jobContext = jobContext;
		this.stepContext = stepContext;
		this.indexingContext = indexingContext;
	}

	private static final Logger logger = Logger.getLogger( ItemWriter.class );

	/**
	 * The checkpointInfo method returns the current checkpoint data for this
	 * writer. It is called before a chunk checkpoint is committed.
	 *
	 * @return the checkpoint info
	 * @throws Exception is thrown for any errors.
	 */
	@Override
	public Serializable checkpointInfo() throws Exception {
		logger.info( "checkpointInfo called" );
		return null;
	}

	/**
	 * The close method marks the end of use of the ItemWriter. The writer is
	 * used to do the cleanup.
	 *
	 * @throws Exception is thrown for any errors.
	 */
	@Override
	public void close() throws Exception {
		logger.info( "close() called" );
	}

	/**
	 * The open method prepares the writer to write items.
	 *
	 * @param checkpoint the last checkpoint
	 */
	@Override
	public void open(Serializable checkpoint) throws Exception {
		logger.info( "open(Seriliazable) called" );
		monitor = new SimpleIndexingProgressMonitor();
	}

	/**
	 * Execute {@code LuceneWork}
	 *
	 * @param items a list of items, where each item is a list of Lucene works.
	 * @throw Exception is thrown for any errors.
	 */
	@Override
	public void writeItems(List<Object> items) throws Exception {

		// TODO: is the sharding strategy used suitable for the situation ?
		IndexShardingStrategy shardingStrategy = ( (EntityIndexingStepData) stepContext
				.getTransientUserData() ).getShardingStrategy();

		for ( Object item : items ) {
			AddLuceneWork addWork = (AddLuceneWork) item;
			StreamingOperationExecutor executor = addWork.acceptIndexWorkVisitor(
					StreamingOperationExecutorSelector.INSTANCE, null );
			executor.performStreamOperation(
					addWork,
					shardingStrategy,
					// monitor,
					null,
					forceAsync );
		}

		// flush after write operation
		Class<?> entityClazz = ( (BatchContextData) jobContext.getTransientUserData() )
				.getIndexedType( entityType );
		EntityIndexBinding indexBinding = Search
				.getFullTextEntityManager( indexingContext.getEntityManager() )
				.getSearchFactory()
				.unwrap( SearchIntegrator.class )
				.getIndexBinding( entityClazz );
		IndexManager[] indexManagers = indexBinding.getIndexManagers();
		for ( IndexManager im : indexManagers ) {
			im.performStreamOperation( FlushLuceneWork.INSTANCE, null, false );
		}
	}
}