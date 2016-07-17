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
import javax.batch.runtime.Metric.MetricType;
import javax.batch.runtime.StepExecution;
import javax.inject.Inject;

import org.hibernate.search.jsr352.test.entity.Company;
import org.hibernate.search.jsr352.test.entity.CompanyManager;
import org.hibernate.search.jsr352.test.entity.MyDate;
import org.hibernate.search.jsr352.test.entity.MyDateManager;
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
 * This integration test (IT) aims to test the mass-indexer job execution under
 * Java EE environment, with step partitioning (parallelism), checkpointing, 
 * restartability and entity composite PK handling mechanism.
 *
 * @author Mincong Huang
 */
@RunWith(Arquillian.class)
public class MassIndexerIT {

	private static final Logger logger = Logger.getLogger( MassIndexerIT.class );

	private final boolean JOB_OPTIMIZE_AFTER_PURGE = false;
	private final boolean JOB_OPTIMIZE_AT_END = false;
	private final boolean JOB_PURGE_AT_START = false;
	private final int JOB_FETCH_SIZE = 100 * 1000;
	private final int JOB_MAX_RESULTS = 200 * 1000;
	private final int JOB_MAX_THREADS = 1;
	private final int JOB_PARTITION_CAPACITY = 1000;
	private final String JOB_PU_NAME = "h2";

	private final long DB_COMP_ROWS = 5000;
	private final long DB_DATE_ROWS = 31; // 2016.07.01 - 2016.07.31

	@Inject
	private CompanyManager companyManager;

	@Inject
	private MyDateManager myDateManager;

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

		final String google = "google";
		final String sunday = "sun";

		// Before the job start, insert data and
		// make sure search result is empty without index
		insertData();
		List<Company> companies = companyManager.findCompanyByName( google );
		List<MyDate> sundays = myDateManager.findDateByWeekday( sunday );
		assertEquals( 0, companies.size() );
		assertEquals( 0, sundays.size() );

		// Start the job. This is the 1st execution.
		JobOperator jobOperator = BatchRuntime.getJobOperator();
		long execId1 = createAndStartJob( jobOperator );
		JobExecution jobExec1 = jobOperator.getJobExecution( execId1 );

		// Stop the job
		BatchTestHelper.stopJobExecution( jobExec1 );
		jobExec1 = BatchTestHelper.keepTestAlive( jobExec1 );
		assertEquals( BatchStatus.STOPPED, jobExec1.getBatchStatus() );
		companies = companyManager.findCompanyByName( google );
		sundays = myDateManager.findDateByWeekday( sunday );
		logger.infof( "After the 1st exec, %d companies found", companies.size() );
		logger.infof( "After the 1st exec, %d dates found", sundays.size() );

		// Restart the job. This is the 2nd execution.
		long execId2 = jobOperator.restart( execId1, null );
		JobExecution jobExec2 = jobOperator.getJobExecution( execId2 );
		jobOperator.getStepExecutions( execId2 )
				.forEach( stepExec -> testBatchStatus( stepExec ) );
		jobExec2 = BatchTestHelper.keepTestAlive( jobExec2 );
		assertEquals( BatchStatus.COMPLETED, jobExec2.getBatchStatus() );

		// After the job execution, test again : results should be found this
		// time. By the way, 5 Sundays will be found in July 2016
		companies = companyManager.findCompanyByName( google );
		sundays = myDateManager.findDateByWeekday( sunday );
		logger.infof( "After the 2nd exec, %d companies found", companies.size() );
		logger.infof( "After the 2nd exec, %d dates found", sundays.size() );
		assertEquals( DB_COMP_ROWS / 5, companies.size() );
		assertEquals( 5, sundays.size() );
	}

	private void testBatchStatus(StepExecution stepExecution) {
		BatchStatus batchStatus = stepExecution.getBatchStatus();
		String stepName = stepExecution.getStepName();
		if ( stepName.equals( "produceLuceneDoc" ) ) {
			Metric[] metrics = stepExecution.getMetrics();
			Map<MetricType, Long> map = BatchTestHelper.getMetricsMap( metrics );
			final long readCount = map.get( MetricType.READ_COUNT );
			final long writeCount = map.get( MetricType.WRITE_COUNT );
			final long expected = DB_COMP_ROWS + DB_DATE_ROWS;
			assertEquals( expected, readCount );
			assertEquals( expected, writeCount );
			assertEquals( BatchStatus.COMPLETED, batchStatus );
		} else {
			String msg = "Unknown step " + stepName;
			throw new IllegalStateException( msg );
		}
	}

	private void insertData() {
		final String[][] str = new String[][]{
				{ "Google", "Sundar", "Pichai" },
				{ "Red Hat", "James", "M. Whitehurst" },
				{ "Microsoft", "Satya", "Nadella" },
				{ "Facebook", "Mark", "Zuckerberg" },
				{ "Amazon", "Jeff", "Bezos" }
		};
		for ( int i = 0; i < DB_COMP_ROWS; i++ ) {
			companyManager.persist( new Company( str[i % 5][0] ) );
		}
		for ( int day = 1; day <= DB_DATE_ROWS; day++ ) {
			logger.info( ( new MyDate( 2016, 07, day ) ).toString() );
			myDateManager.persist( new MyDate( 2016, 07, day ) );
		}
	}

	private long createAndStartJob(JobOperator jobOperator) {
		MassIndexer massIndexer = new MassIndexerImpl()
				.fetchSize( JOB_FETCH_SIZE )
				.maxResults( JOB_MAX_RESULTS )
				.optimizeAfterPurge( JOB_OPTIMIZE_AFTER_PURGE )
				.optimizeAtEnd( JOB_OPTIMIZE_AT_END )
				.partitionCapacity( JOB_PARTITION_CAPACITY )
				.purgeAtStart( JOB_PURGE_AT_START )
				.maxThreads( JOB_MAX_THREADS )
				.entityManagerProvider( JOB_PU_NAME )
				.jobOperator( jobOperator )
				.addRootEntities( Company.class, MyDate.class );
		long executionId = massIndexer.start();
		return executionId;
	}
}
