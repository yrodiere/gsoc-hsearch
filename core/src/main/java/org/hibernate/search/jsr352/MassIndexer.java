/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import java.util.Set;

import javax.batch.operations.JobOperator;
import javax.persistence.EntityManager;

/**
 *
 * @author Mincong HUANG
 */
public interface MassIndexer {

	public long start();

	public void stop(long executionId);

	public MassIndexer addRootEntities(Class<?>... rootEntities);

	public MassIndexer addRootEntities(Set<Class<?>> rootEntities);

	public MassIndexer arrayCapacity(int arrayCapacity);

	/**
	 * Checkpoint frequency during the mass index process. The checkpoint will
	 * be done every N items read, where N is the given item count.
	 *
	 * @param itemCount the number of item count before starting the next
	 * checkpoint.
	 * @return
	 */
	public MassIndexer checkpointFreq(int itemCount);

	// TODO: should be reviewed
	public MassIndexer entityManager(EntityManager entityManager);

	public MassIndexer fetchSize(int fetchSize);

	public MassIndexer jobOperator(JobOperator jobOperator);

	public MassIndexer maxResults(int maxResults);

	public MassIndexer optimizeAfterPurge(boolean optimizeAfterPurge);

	public MassIndexer optimizeAtEnd(boolean optimizeAtEnd);

	public MassIndexer partitionCapacity(int partitionCapacity);

	public MassIndexer partitions(int partitions);

	public MassIndexer purgeAtStart(boolean purgeAtStart);

	public MassIndexer threads(int threads);

	public int getArrayCapacity();

	public int getFetchSize();

	public int getItemCount();

	public int getMaxResults();

	public int getPartitionCapacity();

	public int getPartitions();

	public int getThreads();

	public boolean isOptimizeAfterPurge();

	public boolean isOptimizeAtEnd();

	public boolean isPurgeAtStart();

	public Set<Class<?>> getRootEntities();

	public EntityManager getEntityManager();

	public JobOperator getJobOperator();
}
