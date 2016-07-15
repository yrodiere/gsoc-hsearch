/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.inject.Inject;

import org.hibernate.search.jsr352.test.entity.Company;
import org.hibernate.search.jsr352.test.entity.CompanyManager;
import org.hibernate.search.jsr352.test.util.BatchTestHelper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Mincong Huang
 */
@RunWith(Arquillian.class)
public class MassIndexerIT {

	private final boolean OPTIMIZE_AFTER_PURGE = true;
	private final boolean OPTIMIZE_AT_END = true;
	private final boolean PURGE_AT_START = true;
	private final int ARRAY_CAPACITY = 500;
	private final int FETCH_SIZE = 100000;
	private final int MAX_RESULTS = 200 * 1000;
	private final int MAX_THREADS = 1;
	private final int PARTITION_CAPACITY = 250;

	private final long DB_COMP_ROWS = 1000;

	@Inject
	private CompanyManager companyManager;

	private final String[][] str = new String[][]{
			{ "Google", "Sundar", "Pichai" },
			{ "Red Hat", "James", "M. Whitehurst" },
			{ "Microsoft", "Satya", "Nadella" },
			{ "Facebook", "Mark", "Zuckerberg" },
			{ "Amazon", "Jeff", "Bezos" }
	};

	private static final Logger logger = Logger.getLogger( MassIndexerIT.class );

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive war = ShrinkWrap.create( WebArchive.class )
				.addPackages( true, "org.hibernate.search.jsr352" )
				.addPackages( true, "javax.persistence" )
				.addPackages( true, "org.hibernate.search.annotations" )
				.addClass( Serializable.class )
				.addClass( Date.class )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addAsResource( "META-INF/persistence.xml" )
				.addAsResource( "META-INF/batch-jobs/mass-index.xml" );
		return war;
	}

	@Test
	public void testJob() throws InterruptedException {

		// Before the job start, insert data and
		// make sure search result is empty without index
		for ( int i = 0; i < DB_COMP_ROWS; i++ ) {
			companyManager.persist( new Company( str[i % 5][0] ) );
		}
		final String keyword = "google";
		List<Company> companies = companyManager.findCompanyByName( keyword );
		assertEquals( 0, companies.size() );

		// start job and test it
		// with different metrics obtained
		JobOperator jobOperator = BatchRuntime.getJobOperator();
		MassIndexer massIndexer = createAndInitJob( jobOperator );
		long executionId = massIndexer.start();

		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		jobExecution = BatchTestHelper.keepTestAlive( jobExecution );
		jobOperator.getStepExecutions( executionId )
				.forEach( stepExec -> testBatchStatus( stepExec ) );
		assertEquals( jobExecution.getBatchStatus(), BatchStatus.COMPLETED );
		logger.info( "Mass indexing finished" );

		companies = companyManager.findCompanyByName( keyword );
		assertEquals( DB_COMP_ROWS / 5, companies.size() );
	}

	private void testBatchStatus(StepExecution stepExecution) {
		BatchStatus batchStatus = stepExecution.getBatchStatus();
		switch ( stepExecution.getStepName() ) {

			case "loadId":
				long expectedEntityCount = DB_COMP_ROWS;
				// assertEquals( expectedEntityCount,
				// indexingContext.getEntityCount() );
				assertEquals( BatchStatus.COMPLETED, batchStatus );
				break;

			case "purgeDecision":
				assertEquals( BatchStatus.COMPLETED, batchStatus );
				break;

			case "purgeIndex":
				if ( PURGE_AT_START ) {
					assertEquals( BatchStatus.COMPLETED, batchStatus );
				}
				break;

			case "afterPurgeDecision":
				assertEquals( BatchStatus.COMPLETED, batchStatus );
				break;

			case "optimizeAfterPurge":
				if ( OPTIMIZE_AFTER_PURGE ) {
					assertEquals( BatchStatus.COMPLETED, batchStatus );
				}
				break;

			case "produceLuceneDoc":
				Metric[] metrics = stepExecution.getMetrics();
				testChunk( BatchTestHelper.getMetricsMap( metrics ) );
				assertEquals( BatchStatus.COMPLETED, batchStatus );
				break;

			case "afterIndexDecision":
				assertEquals( BatchStatus.COMPLETED, batchStatus );
				break;

			case "optimizeAfterIndex":
				assertEquals( BatchStatus.COMPLETED, batchStatus );
				break;

			default:
				break;
		}
	}

	private void testChunk(Map<Metric.MetricType, Long> metricsMap) {
		assertEquals( DB_COMP_ROWS, metricsMap.get( Metric.MetricType.READ_COUNT ).longValue() );
		assertEquals( DB_COMP_ROWS, metricsMap.get( Metric.MetricType.WRITE_COUNT ).longValue() );
	}

	private MassIndexer createAndInitJob(JobOperator jobOperator) {
		MassIndexer massIndexer = new MassIndexerImpl()
				.arrayCapacity( ARRAY_CAPACITY )
				.fetchSize( FETCH_SIZE )
				.maxResults( MAX_RESULTS )
				.optimizeAfterPurge( OPTIMIZE_AFTER_PURGE )
				.optimizeAtEnd( OPTIMIZE_AT_END )
				.partitionCapacity( PARTITION_CAPACITY )
				.purgeAtStart( PURGE_AT_START )
				.maxThreads( MAX_THREADS )
				.entityManagerProvider( "h2" )
				.jobOperator( jobOperator )
				.addRootEntities( Company.class );
		return massIndexer;
	}
}
