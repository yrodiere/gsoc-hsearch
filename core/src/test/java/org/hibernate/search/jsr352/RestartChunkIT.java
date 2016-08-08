/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.jsr352.MassIndexerImpl;
import org.hibernate.search.jsr352.entity.Company;
import org.hibernate.search.jsr352.entity.Person;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Mincong Huang
 */
@Ignore("Issue #96 EntityManager and TX cannot be handled correctly in Java SE")
public class RestartChunkIT {

	private static final Logger LOGGER = Logger.getLogger( RestartChunkIT.class );

	private final long DB_COMP_ROWS = 100;
	private final long DB_PERS_ROWS = 50;
	private final int JOB_MAX_TRIES = 30; // 1s * 30 = 30s
	private final int JOB_THREAD_SLEEP = 1000; // 1s

	private JobOperator jobOperator;
	private EntityManagerFactory emf;

	@Before
	public void setup() {

		String[][] str = new String[][]{
				{ "Google", "Sundar", "Pichai" },
				{ "Red Hat", "James", "M. Whitehurst" },
				{ "Microsoft", "Satya", "Nadella" },
				{ "Facebook", "Mark", "Zuckerberg" },
				{ "Amazon", "Jeff", "Bezos" }
		};

		jobOperator = JobFactory.getJobOperator();
		emf = Persistence.createEntityManagerFactory( "h2" );

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		for ( int i = 0; i < DB_COMP_ROWS; i++ ) {
			em.persist( new Company( str[i % 5][0] ) );
		}
		for ( int i = 0; i < DB_PERS_ROWS; i++ ) {
			String firstName = str[i % 5][1];
			String lastName = str[i % 5][2];
			String id = String.format( "%2d%c", i, firstName.charAt( 0 ) );
			em.persist( new Person( id, firstName, lastName ) );
		}
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testJob() throws InterruptedException {

		List<Company> companies = findClasses( Company.class, "name", "Google" );
		List<Person> people = findClasses( Person.class, "firstName", "Sundar" );
		assertEquals( 0, companies.size() );
		assertEquals( 0, people.size() );

		// start the job, then stop it
		long execId1 = new MassIndexerImpl()
				.addRootEntities( Company.class, Person.class )
				.entityManagerFactory( emf )
				.jobOperator( jobOperator )
				.start();
		JobExecution jobExec1 = jobOperator.getJobExecution( execId1 );
		stopChunkAfterStarted( jobExec1 );
		jobExec1 = keepTestAlive( jobExec1 );
		String msg1 = String.format( "Job (executionId=%d) %s, executed steps:%n%n",
				execId1,
				jobExec1.getBatchStatus() );
		List<StepExecution> stepExecs1 = jobOperator.getStepExecutions( execId1 );
		for ( StepExecution stepExec : stepExecs1 ) {
			boolean isRestarted = false;
			testBatchStatus( stepExec, isRestarted );
			msg1 += String.format( "\tid=%s, status=%s%n",
					stepExec.getStepName(),
					stepExec.getBatchStatus() );
		}
		LOGGER.info( msg1 );

		// restart the job
		long execId2 = jobOperator.restart( execId1, null );
		JobExecution jobExec2 = jobOperator.getJobExecution( execId2 );
		jobExec2 = keepTestAlive( jobExec2 );
		String msg2 = String.format( "Job (executionId=%d) stopped, executed steps:%n%n", execId2 );
		List<StepExecution> stepExecs2 = jobOperator.getStepExecutions( execId2 );
		for ( StepExecution stepExec : stepExecs2 ) {
			boolean isRestarted = true;
			testBatchStatus( stepExec, isRestarted );
			msg2 += String.format( "\tid=%s, status=%s%n",
					stepExec.getStepName(),
					stepExec.getBatchStatus() );
		}
		LOGGER.info( msg2 );
		LOGGER.info( "finished" );

		// search again
		companies = findClasses( Company.class, "name", "google" );
		people = findClasses( Person.class, "firstName", "Sundar" );
		assertEquals( DB_COMP_ROWS / 5, companies.size() );
		assertEquals( DB_PERS_ROWS / 5, people.size() );
	}

	private <T> List<T> findClasses(Class<T> clazz, String key, String value) {
		EntityManager em = emf.createEntityManager();
		FullTextEntityManager ftem = Search.getFullTextEntityManager( em );
		Query luceneQuery = ftem.getSearchFactory().buildQueryBuilder()
				.forEntity( clazz ).get()
				.keyword().onField( key ).matching( value )
				.createQuery();
		@SuppressWarnings("unchecked")
		List<T> result = ftem.createFullTextQuery( luceneQuery ).getResultList();
		em.close();
		return result;
	}

	private JobExecution keepTestAlive(JobExecution jobExecution) throws InterruptedException {
		int tries = 0;
		while ( !jobExecution.getBatchStatus().equals( BatchStatus.COMPLETED )
				&& !jobExecution.getBatchStatus().equals( BatchStatus.STOPPED )
				&& tries < JOB_MAX_TRIES ) {

			long executionId = jobExecution.getExecutionId();
			LOGGER.infof( "Job (id=%d) %s, thread sleep %d ms...",
					executionId,
					jobExecution.getBatchStatus(),
					JOB_THREAD_SLEEP );
			Thread.sleep( JOB_THREAD_SLEEP );
			jobExecution = jobOperator.getJobExecution( executionId );
			tries++;
		}
		return jobExecution;
	}

	private void stopChunkAfterStarted(JobExecution jobExecution) throws InterruptedException {

		int tries = 0;
		long executionId = jobExecution.getExecutionId();
		List<StepExecution> stepExecutions = jobOperator.getStepExecutions( executionId );
		LOGGER.infof( "%d steps found", stepExecutions.size() );
		Iterator<StepExecution> cursor = stepExecutions.iterator();
		while ( !jobExecution.getBatchStatus().equals( BatchStatus.COMPLETED )
				|| !jobExecution.getBatchStatus().equals( BatchStatus.FAILED )
				|| tries < JOB_MAX_TRIES ) {

			// find step "produceLuceneDoc"
			while ( cursor.hasNext() ) {

				StepExecution stepExecution = cursor.next();
				String stepName = stepExecution.getStepName();
				BatchStatus stepStatus = stepExecution.getBatchStatus();

				if ( stepName.equals( "produceLuceneDoc" ) ) {
					LOGGER.info( "step produceLuceneDoc found." );
					if ( stepStatus.equals( BatchStatus.STARTING ) ) {
						LOGGER.info( "step status is STARTING, wait it until STARTED to stop" );
						break;
					}
					else {
						LOGGER.infof( "step status is %s, stopping now ...", stepStatus );
						jobOperator.stop( executionId );
						return;
					}
				}
			}
			Thread.sleep( 200 );
			tries++;
			stepExecutions = jobOperator.getStepExecutions( executionId );
			cursor = stepExecutions.iterator();
		}
	}

	private void testBatchStatus(StepExecution stepExecution, boolean isRestarted) {
		BatchStatus batchStatus = stepExecution.getBatchStatus();
		switch ( stepExecution.getStepName() ) {

			case "loadId":
				// long expectedEntityCount = DB_COMP_ROWS;
				// assertEquals(expectedEntityCount,
				// indexingContext.getEntityCount());
				assertEquals( BatchStatus.COMPLETED, batchStatus );
				break;

			case "produceLuceneDoc":
				String msg = String.format( "metrics in step produceLuceneDoc:%n%n" );
				Metric[] metrics = stepExecution.getMetrics();
				for ( Metric metric : metrics ) {
					msg += String.format( "\t%s: %d%n", metric.getType(), metric.getValue() );
				}
				LOGGER.info( msg );
				if ( isRestarted ) {
					// TODO: enable to below test after code enhancement
					// testChunk(getMetricsMap(metrics));
					assertEquals( BatchStatus.COMPLETED, batchStatus );
				}
				else {
					// first execution should be stopped
					assertEquals( BatchStatus.STOPPED, batchStatus );
				}
				break;

			default:
				break;
		}
	}

	private void testChunk(Map<Metric.MetricType, Long> metricsMap) {
		long companyCount = DB_COMP_ROWS;
		// The read count.
		long expectedReadCount = companyCount;
		long actualReadCount = metricsMap.get( Metric.MetricType.READ_COUNT );
		assertEquals( expectedReadCount, actualReadCount );
		// The write count
		long expectedWriteCount = companyCount;
		long actualWriteCount = metricsMap.get( Metric.MetricType.WRITE_COUNT );
		assertEquals( expectedWriteCount, actualWriteCount );
	}

	/**
	 * Convert the Metric array contained in StepExecution to a key-value map
	 * for easy access to Metric parameters.
	 *
	 * @param metrics a Metric array contained in StepExecution.
	 * @return a map view of the metrics array.
	 */
	private Map<Metric.MetricType, Long> getMetricsMap(Metric[] metrics) {
		Map<Metric.MetricType, Long> metricsMap = new HashMap<>();
		for ( Metric metric : metrics ) {
			metricsMap.put( metric.getType(), metric.getValue() );
		}
		return metricsMap;
	}

	@After
	public void shutdownJPA() {
		emf.close();
	}
}
