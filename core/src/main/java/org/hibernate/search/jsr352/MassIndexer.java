/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import java.util.Set;

import javax.batch.operations.JobOperator;
import javax.persistence.EntityManagerFactory;

/**
 * An alternative interface to the current mass indexer, using the Java Batch
 * architecture as defined by JSR 352.
 *
 * @author Mincong Huang
 */
public interface MassIndexer {

	/**
	 * Start the job.
	 *
	 * @return
	 */
	public long start();

	/**
	 * Stop the job.
	 *
	 * @param executionId
	 */
	public void stop(long executionId);

	/**
	 * Add entity type to index.
	 *
	 * @param rootEntitiy
	 * @return
	 */
	public MassIndexer addRootEntity(Class<?> rootEntitiy);

	/**
	 * Add entity types to index. Currently, only root entities are accepted
	 * because the lack of entity types retrieve inside the job.
	 *
	 * @param rootEntities
	 * @return
	 */
	public MassIndexer addRootEntities(Class<?>... rootEntities);

	/**
	 * Checkpoint frequency during the mass index process. The checkpoint will
	 * be done every N items read, where N is the given item count.
	 *
	 * @param itemCount the number of item count before starting the next
	 * checkpoint.
	 * @return
	 */
	public MassIndexer checkpointFreq(int itemCount);

	/**
	 * Whether the Hibernate queries are cacheable. This setting will be applied
	 * to all the queries. The default value is false.
	 *
	 * @param cacheable
	 * @return
	 */
	public MassIndexer cacheable(boolean cacheable);

	/**
	 * Assign the entity manager factory. You must use this method if you're
	 * under Java SE. You should NOT use it if you're under Java EE.
	 *
	 * @param entityManagerFactory
	 * @return
	 */
	public MassIndexer entityManagerFactory(EntityManagerFactory entityManagerFactory);

	/**
	 * The fetch size for the result fetching.
	 *
	 * @param fetchSize
	 * @return
	 */
	public MassIndexer fetchSize(int fetchSize);

	/**
	 * Whether the a Java SE environment. Default is false.
	 *
	 * @param isJavaSE
	 * @return
	 */
	public MassIndexer isJavaSE(boolean isJavaSE);

	/**
	 * Job operator to start the batch job.
	 *
	 * @param jobOperator
	 * @return
	 */
	public MassIndexer jobOperator(JobOperator jobOperator);

	/**
	 * The maximum number of results will be return from the HQL / criteria. It
	 * is equivalent to keyword `LIMIT` in SQL.
	 *
	 * @param maxResults
	 * @return
	 */
	public MassIndexer maxResults(int maxResults);

	/**
	 * Specify the maximum number of threads on which to execute the partitions
	 * of this step. Note the batch runtime cannot guarantee the request number
	 * of threads are available; it will use as many as it can up to the request
	 * maximum. This an an optional attribute. The default is the number of
	 * partitions.
	 *
	 * @param maxThreads
	 * @return
	 */
	public MassIndexer maxThreads(int maxThreads);

	/**
	 * Specify whether the mass indexer should be optimized at the beginning of
	 * the job. This operation takes place after the purge operation and before
	 * the step of lucene document production. The default value is false. TODO:
	 * specify what is the optimization exactly
	 *
	 * @param optimizeAfterPurge
	 * @return
	 */
	public MassIndexer optimizeAfterPurge(boolean optimizeAfterPurge);

	/**
	 * Specify whether the mass indexer should be optimized at the end of the
	 * job. This operation takes place after the step of lucene document
	 * production. The default value is false. TODO: specify what is the
	 * optimization exactly
	 *
	 * @param optimizeAtEnd
	 * @return
	 */
	public MassIndexer optimizeAtEnd(boolean optimizeAtEnd);

	/**
	 * Specify whether the existing lucene index should be purged at the
	 * beginning of the job. This operation takes place before the step of
	 * lucene document production. The default value is false.
	 *
	 * @param purgeAtStart
	 * @return
	 */
	public MassIndexer purgeAtStart(boolean purgeAtStart);

	/**
	 * Define the max number of rows to process per partition.
	 *
	 * @param partitionCapacity
	 * @return
	 */
	public MassIndexer rowsPerPartition(int rowsPerPartition);

	public int getFetchSize();

	public int getItemCount();

	public int getMaxResults();

	public int getMaxThreads();

	public int getRowsPerPartition();

	public boolean cacheable();

	public boolean isOptimizeAfterPurge();

	public boolean isOptimizeAtEnd();

	public boolean isPurgeAtStart();

	public Set<Class<?>> getRootEntities();

	public JobOperator getJobOperator();
}
