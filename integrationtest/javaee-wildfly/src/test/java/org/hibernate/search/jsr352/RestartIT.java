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

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.inject.Inject;

import org.hibernate.search.jsr352.test.entity.Company;
import org.hibernate.search.jsr352.test.entity.CompanyManager;
import org.hibernate.search.jsr352.test.entity.Person;
import org.hibernate.search.jsr352.test.entity.PersonManager;
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
 * This integration test (IT) aims to test the restartability of the job
 * execution mass-indexer under Java EE environment, with step partitioning
 * (parallelism). We need to prove that the job restart from the checkpoint
 * where it was stopped, but not from the very beginning.
 *
 * @author Mincong Huang
 */
@RunWith(Arquillian.class)
public class RestartIT {

	private static final Logger logger = Logger.getLogger( RestartIT.class );

	private final boolean JOB_PURGE_AT_START = true;
	private final int JOB_FETCH_SIZE = 100 * 1000;
	private final int JOB_MAX_RESULTS = 200 * 1000;
	private final int JOB_MAX_THREADS = 3;
	private final int JOB_PARTITION_CAPACITY = 1000;
	private final String JOB_PU_NAME = "h2";

	private final long DB_COMP_ROWS = 2500;
	private final long DB_PERS_ROWS = 2500;

	@Inject
	private CompanyManager companyManager;

	@Inject
	private PersonManager personManager;

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive war = ShrinkWrap.create( WebArchive.class )
				.addAsResource( "META-INF/persistence.xml" )
				.addAsResource( "META-INF/batch-jobs/mass-index.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addClasses( Serializable.class, Date.class )
				.addPackages( true, "org.hibernate.search.annotations" )
				.addPackages( true, "org.hibernate.search.jsr352" )
				.addPackages( true, "javax.persistence" );
		return war;
	}

	@Test
	public void testJob() throws InterruptedException {

		final String google = "google";
		final String googleCEO = "Sundar";

		insertData();
		List<Company> googles = companyManager.findCompanyByName( google );
		List<Person> googleCEOs = personManager.findPerson( googleCEO );
		assertEquals( 0, googles.size() );
		assertEquals( 0, googleCEOs.size() );

		// Start the job. This is the 1st execution.
		// Keep the execution alive and wait Byteman to stop the job
		JobOperator jobOperator = BatchRuntime.getJobOperator();
		long execId1 = createAndStartJob( jobOperator );
		JobExecution jobExec1 = jobOperator.getJobExecution( execId1 );
		jobExec1 = BatchTestHelper.keepTestAlive( jobExec1 );

		// Restart the job. This is the 2nd execution.
		long execId2 = jobOperator.restart( execId1, null );
		JobExecution jobExec2 = jobOperator.getJobExecution( execId2 );
		jobExec2 = BatchTestHelper.keepTestAlive( jobExec2 );
		assertEquals( BatchStatus.COMPLETED, jobExec2.getBatchStatus() );

		googles = companyManager.findCompanyByName( google );
		googleCEOs = personManager.findPerson( googleCEO );
		assertEquals( DB_COMP_ROWS / 5, googles.size() );
		assertEquals( DB_PERS_ROWS / 5, googleCEOs.size() );

		final long nbCompanies = companyManager.rowCount();
		final long nbPeople = personManager.rowCount();
		assertEquals( DB_COMP_ROWS, nbCompanies );
		assertEquals( DB_PERS_ROWS, nbPeople );
	}

	private void insertData() {
		final String[][] str = new String[][]{
				{ "Google", "Sundar", "Pichai" },
				{ "Red Hat", "James", "M. Whitehurst" },
				{ "Microsoft", "Satya", "Nadella" },
				{ "Facebook", "Mark", "Zuckerberg" },
				{ "Amazon", "Jeff", "Bezos" }
		};
		for ( int i = 0; i < DB_PERS_ROWS; i++ ) {
			Person p = new Person( i, str[i % 5][1], str[i % 5][2] );
			personManager.persist( p );
		}
		for ( int i = 0; i < DB_COMP_ROWS; i++ ) {
			Company c = new Company( str[i % 5][0] );
			companyManager.persist( c );
		}
	}

	private long createAndStartJob(JobOperator jobOperator) {
		MassIndexer massIndexer = new MassIndexerImpl()
				.fetchSize( JOB_FETCH_SIZE )
				.maxResults( JOB_MAX_RESULTS )
				.maxThreads( JOB_MAX_THREADS )
				.partitionCapacity( JOB_PARTITION_CAPACITY )
				.purgeAtStart( JOB_PURGE_AT_START )
				.entityManagerProvider( JOB_PU_NAME )
				.jobOperator( jobOperator )
				.addRootEntities( Company.class, Person.class );
		long executionId = massIndexer.start();
		return executionId;
	}
}
