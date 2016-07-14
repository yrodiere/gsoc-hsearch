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
import javax.naming.InitialContext;
import javax.persistence.EntityManager;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.impl.StreamingOperationExecutor;
import org.hibernate.search.backend.impl.StreamingOperationExecutorSelector;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.internal.JobContextData;
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
 * @author Mincong Huang
 */
@Named
public class ItemWriter implements javax.batch.api.chunk.ItemWriter {

	private final Boolean forceAsync = true;
	private final JobContext jobContext;
	private final StepContext stepContext;
	private EntityManager em;
	private EntityIndexBinding entityIndexBinding;

	@Inject
	@BatchProperty
	private String entityName;

	@Inject
	@BatchProperty
	private String persistenceUnitName;

	@Inject
	public ItemWriter(JobContext jobContext, StepContext stepContext) {
		this.jobContext = jobContext;
		this.stepContext = stepContext;
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
		logger.info( "close() called." );
	}

	/**
	 * The open method prepares the writer to write items.
	 *
	 * @param checkpoint the last checkpoint
	 */
	@Override
	public void open(Serializable checkpoint) throws Exception {

		logger.info( "open(Seriliazable) called" );
		String path = "java:comp/env/" + persistenceUnitName;
		em = (EntityManager) InitialContext.doLookup( path );

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		Class<?> entityClazz = jobData.getIndexedType( entityName );
		entityIndexBinding = Search
				.getFullTextEntityManager( em )
				.getSearchFactory()
				.unwrap( SearchIntegrator.class )
				.getIndexBinding( entityClazz );

		if ( stepContext.getPersistentUserData() == null ) {
			stepContext.setPersistentUserData( new StepContextData() );
		}
	}

	/**
	 * Execute {@code LuceneWork}
	 *
	 * @param items a list of items, where each item is a list of Lucene works.
	 * @throw Exception is thrown for any errors.
	 */
	@Override
	public void writeItems(List<Object> items) throws Exception {
		IndexShardingStrategy shardingStrategy = entityIndexBinding.getSelectionStrategy();

		for ( Object item : items ) {
			AddLuceneWork addWork = (AddLuceneWork) item;
			StreamingOperationExecutor executor = addWork.acceptIndexWorkVisitor(
					StreamingOperationExecutorSelector.INSTANCE, null );
			executor.performStreamOperation(
					addWork,
					shardingStrategy,
					null, // monitor,
					forceAsync );
		}

		// flush after write operation
		IndexManager[] indexManagers = entityIndexBinding.getIndexManagers();
		for ( IndexManager im : indexManagers ) {
			im.performStreamOperation( FlushLuceneWork.INSTANCE, null, false );
		}

		// update work count
		StepContextData stepContextData = (StepContextData) stepContext.getPersistentUserData();
		stepContextData.setChunkWorkCount( items.size() );
		stepContext.setPersistentUserData( stepContextData );
	}
}
