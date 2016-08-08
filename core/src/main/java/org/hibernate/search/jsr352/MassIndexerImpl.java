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
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.jsr352.internal.se.JobSEEnvironment;

/**
 * @author Mincong Huang
 */
public class MassIndexerImpl implements MassIndexer {

	private final String JOB_NAME = "mass-index";
	private final Set<Class<?>> rootEntities = new HashSet<>();

	private boolean cacheable = false;
	private boolean optimizeAfterPurge = false;
	private boolean optimizeAtEnd = false;
	private boolean purgeAtStart = false;
	private boolean isJavaSE = false;
	private int fetchSize = 200 * 1000;
	private int itemCount = 3;
	private int maxResults = 1000 * 1000;
	private int rowsPerPartition = 250;
	private int maxThreads = 1;
	private JobOperator jobOperator;

	public MassIndexerImpl() {

	}

	@Override
	public long start() {

		if ( rootEntities == null ) {
			throw new NullPointerException( "rootEntities cannot be null" );
		}
		if ( isJavaSE ) {
			if ( JobSEEnvironment.getEntityManagerFactory() == null ) {
				throw new NullPointerException( "You're under a Java SE environment. "
						+ "Please assign the EntityManagerFactory via method "
						+ "MassIndexer#setEntityManagerFactory(EntityManagerFactory) "
						+ "before the job start." );
			}
		}
		else {
			if ( JobSEEnvironment.getEntityManagerFactory() != null ) {
				throw new IllegalStateException( "You're under a Java EE environmant. "
						+ "Please do not assign the EntityManagerFactory to the mass indexer." );
			}
		}

		Properties jobParams = new Properties();
		jobParams.put( "fetchSize", String.valueOf( fetchSize ) );
		jobParams.put( "isJavaSE", String.valueOf( isJavaSE ) );
		jobParams.put( "itemCount", String.valueOf( itemCount ) );
		jobParams.put( "maxResults", String.valueOf( maxResults ) );
		jobParams.put( "maxThreads", String.valueOf( maxThreads ) );
		jobParams.put( "optimizeAfterPurge", String.valueOf( optimizeAfterPurge ) );
		jobParams.put( "optimizeAtEnd", String.valueOf( optimizeAtEnd ) );
		jobParams.put( "purgeAtStart", String.valueOf( purgeAtStart ) );
		jobParams.put( "rootEntities", getRootEntitiesAsString() );
		jobParams.put( "rowsPerPartition", String.valueOf( rowsPerPartition ) );
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
	public MassIndexer isJavaSE(boolean isJavaSE) {
		this.isJavaSE = isJavaSE;
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
	public MassIndexer cacheable(boolean cacheable) {
		this.cacheable = cacheable;
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
	public MassIndexer rowsPerPartition(int rowsPerPartition) {
		if ( rowsPerPartition < 1 ) {
			throw new IllegalArgumentException(
					"rowsPerPartition must be at least 1" );
		}
		this.rowsPerPartition = rowsPerPartition;
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
	public MassIndexer addRootEntity(Class<?> rootEntity) {
		if ( rootEntity == null ) {
			throw new NullPointerException( "rootEntity cannot be NULL." );
		}
		this.rootEntities.add( rootEntity );
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
	public MassIndexer entityManagerFactory(EntityManagerFactory entityManagerFactory) {
		if ( entityManagerFactory == null ) {
			throw new NullPointerException( "The entityManagerFactory cannot be null." );
		}
		else if ( !entityManagerFactory.isOpen() ) {
			throw new IllegalStateException( "Please provide an open entityManagerFactory." );
		}
		JobSEEnvironment.setEntityManagerFactory( entityManagerFactory );
		return this;
	}

	@Override
	public MassIndexer jobOperator(JobOperator jobOperator) {
		this.jobOperator = jobOperator;
		return this;
	}

	@Override
	public boolean cacheable() {
		return cacheable;
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
	public int getMaxThreads() {
		return maxThreads;
	}

	@Override
	public int getRowsPerPartition() {
		return rowsPerPartition;
	}

	public String getJOB_NAME() {
		return JOB_NAME;
	}

	@Override
	public Set<Class<?>> getRootEntities() {
		return rootEntities;
	}

	private String getRootEntitiesAsString() {
		return rootEntities.stream()
				.map( (e) -> e.getName() )
				.collect( Collectors.joining( "," ) );
	}

	@Override
	public int getItemCount() {
		return itemCount;
	}

	@Override
	public JobOperator getJobOperator() {
		return jobOperator;
	}
}
