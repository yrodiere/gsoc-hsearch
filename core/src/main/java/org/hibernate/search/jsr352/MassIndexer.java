/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import java.util.Set;

import javax.batch.operations.JobOperator;

/**
 * @author Mincong Huang
 */
public interface MassIndexer {

	public long start();

	public void stop(long executionId);

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
	 * EntityManager will be assigned inside the mass indexer with the JNDI
	 * lookup.
	 *
	 * @param persistenceUnitName the persistence unit name marked in the
	 * persistence.xml file.
	 * @return
	 */
	public MassIndexer entityManagerProvider(String persistenceUnitName);

	public MassIndexer fetchSize(int fetchSize);

	public MassIndexer jobOperator(JobOperator jobOperator);

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

	public MassIndexer optimizeAfterPurge(boolean optimizeAfterPurge);

	public MassIndexer optimizeAtEnd(boolean optimizeAtEnd);

	public MassIndexer partitionCapacity(int partitionCapacity);

	public MassIndexer purgeAtStart(boolean purgeAtStart);

	public int getFetchSize();

	public int getItemCount();

	public int getMaxResults();

	public int getMaxThreads();

	public int getPartitionCapacity();

	public boolean isOptimizeAfterPurge();

	public boolean isOptimizeAtEnd();

	public boolean isPurgeAtStart();

	public Set<Class<?>> getRootEntities();

	public JobOperator getJobOperator();
}
