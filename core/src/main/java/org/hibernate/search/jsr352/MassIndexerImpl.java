/*
é * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;


public class MassIndexerImpl implements MassIndexer {

	private boolean optimizeAfterPurge = false;
	private boolean optimizeAtEnd = false;
	private boolean purgeAtStart = false;
	private int fetchSize = 200 * 1000;
	private int itemCount = 3;
	private int maxResults = 1000 * 1000;
	private int partitionCapacity = 250;
	private int maxThreads = 1;
	private String persistenceUnitName;
	private final Set<Class<?>> rootEntities = new HashSet<>();
	private JobOperator jobOperator;

	private final String JOB_NAME = "mass-index";

	public MassIndexerImpl() {

	}

	/**
	 * Mass index the Address entity's.
	 * <p>
	 * Here're an example with parameters and expected results:
	 * <ul>
	 * <li><b>array capacity</b> = 500
	 * <li><b>partition capacity</b> = 250
	 * <li><b>max results</b> = 200 * 1000
	 * <li><b>queue size</b> = Math.ceil(max results / array capacity) = Math.ceil(200 * 1000 / 500) = Math.ceil(400) =
	 * 400
	 * <li><b>number of partitions</b> = Math.ceil(queue size / partition capacity) = Math.ceil(400 / 250) =
	 * Math.ceil(1.6) = 2
	 * </ul>
	 */
	@Override
	public long start() {
//		registrerEntityManager( entityManager );

		Properties jobParams = new Properties();
		jobParams.setProperty( "fetchSize", String.valueOf( fetchSize ) );
		jobParams.setProperty( "itemCount", String.valueOf( itemCount ) );
		jobParams.setProperty( "maxResults", String.valueOf( maxResults ) );
		jobParams.setProperty( "maxThreads", String.valueOf( maxThreads ) );
		jobParams.setProperty( "optimizeAfterPurge", String.valueOf( optimizeAfterPurge ) );
		jobParams.setProperty( "optimizeAtEnd", String.valueOf( optimizeAtEnd ) );
		jobParams.setProperty( "partitionCapacity", String.valueOf( partitionCapacity ) );
		jobParams.setProperty( "persistenceUnitName", persistenceUnitName );
		jobParams.setProperty( "purgeAtStart", String.valueOf( purgeAtStart ) );
		jobParams.put( "rootEntities", getEntitiesToIndexAsString() );
		// JobOperator jobOperator = BatchRuntime.getJobOperator();
		Long executionId = jobOperator.start( JOB_NAME, jobParams );
		return executionId;
	}

	@Override
	public void stop(long executionId) {
		JobOperator jobOperator = BatchRuntime.getJobOperator();
		jobOperator.stop( executionId );
	}

	@Override
	public MassIndexer fetchSize(int fetchSize) {
		if ( fetchSize < 1 ) {
			throw new IllegalArgumentException( "fetchSize must be at least 1" );
		}
		this.fetchSize = fetchSize;
		return this;
	}

	@Override
	public MassIndexer maxResults(int maxResults) {
		if ( maxResults < 1 ) {
			throw new IllegalArgumentException( "maxResults must be at least 1" );
		}
		this.maxResults = maxResults;
		return this;
	}

	@Override
	public MassIndexer optimizeAfterPurge(boolean optimizeAfterPurge) {
		this.optimizeAfterPurge = optimizeAfterPurge;
		return this;
	}

	@Override
	public MassIndexer optimizeAtEnd(boolean optimizeAtEnd) {
		this.optimizeAtEnd = optimizeAtEnd;
		return this;
	}

	@Override
	public MassIndexer partitionCapacity(int partitionCapacity) {
		if ( partitionCapacity < 1 ) {
			throw new IllegalArgumentException(
					"partitionCapacity must be at least 1" );
		}
		this.partitionCapacity = partitionCapacity;
		return this;
	}

	@Override
	public MassIndexer purgeAtStart(boolean purgeAtStart) {
		this.purgeAtStart = purgeAtStart;
		return this;
	}

	@Override
	public MassIndexer maxThreads(int maxThreads) {
		if ( maxThreads < 1 ) {
			throw new IllegalArgumentException( "threads must be at least 1." );
		}
		this.maxThreads = maxThreads;
		return this;
	}

	@Override
	public MassIndexer addRootEntities(Class<?>... rootEntities) {
		if ( rootEntities == null ) {
			throw new NullPointerException( "rootEntities cannot be NULL." );
		}
		else if ( rootEntities.length == 0 ) {
			throw new IllegalStateException(
					"rootEntities must have at least 1 element." );
		}
		this.rootEntities.addAll( Arrays.asList( rootEntities ) );
		return this;
	}
	
	@Override
	public MassIndexer checkpointFreq(int itemCount) {
		this.itemCount = itemCount;
		return this;
	}

	@Override
	public MassIndexer entityManagerProvider(String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
		return this;
	}

	@Override
	public MassIndexer jobOperator(JobOperator jobOperator) {
		this.jobOperator = jobOperator;
		return this;
	}

	@Override
	public boolean isOptimizeAfterPurge() {
		return optimizeAfterPurge;
	}

	@Override
	public boolean isOptimizeAtEnd() {
		return optimizeAtEnd;
	}

	@Override
	public boolean isPurgeAtStart() {
		return purgeAtStart;
	}

	@Override
	public int getFetchSize() {
		return fetchSize;
	}

	@Override
	public int getMaxResults() {
		return maxResults;
	}

	@Override
	public int getPartitionCapacity() {
		return partitionCapacity;
	}

	@Override
	public int getMaxThreads() {
		return maxThreads;
	}

	public String getJOB_NAME() {
		return JOB_NAME;
	}

	@Override
	public Set<Class<?>> getRootEntities() {
		return rootEntities;
	}

	@Override
	public int getItemCount() {
		return itemCount;
	}

	private String getEntitiesToIndexAsString() {
		return rootEntities.stream()
				.map( (e) -> e.getName() )
				.collect( Collectors.joining( "," ) );
	}

	@Override
	public JobOperator getJobOperator() {
		return jobOperator;
	}
}
