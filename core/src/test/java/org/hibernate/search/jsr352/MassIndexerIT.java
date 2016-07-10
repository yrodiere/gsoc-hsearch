/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
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
import org.hibernate.search.jsr352.MassIndexer;
import org.hibernate.search.jsr352.MassIndexerImpl;
import org.hibernate.search.jsr352.entity.Company;
import org.hibernate.search.jsr352.entity.Person;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Mincong HUANG
 */
public class MassIndexerIT {

	private EntityManagerFactory emf;

	private JobOperator jobOperator;

	// mass indexer configuration values
	private final boolean OPTIMIZE_AFTER_PURGE = true;
	private final boolean OPTIMIZE_AT_END = true;
	private final boolean PURGE_AT_START = true;
	private final int ARRAY_CAPACITY = 500;
	private final int FETCH_SIZE = 100000;
	private final int MAX_RESULTS = 200 * 1000;
	private final int PARTITION_CAPACITY = 250;
	private final int PARTITIONS = 1;
	private final int THREADS = 1;

	// example dataset
	private final long DB_COMP_ROWS = 3;
	private final long DB_COMP_ROWS_LOADED = 3;

	private static final int JOB_MAX_TRIES = 240; // 240 second
	private static final int JOB_THREAD_SLEEP = 1000;

	private static final Logger logger = Logger.getLogger( MassIndexerIT.class );

	@Before
	public void setup() {

		jobOperator = JobFactory.getJobOperator();
		emf = Persistence.createEntityManagerFactory( "h2" );

		List<Company> companies = Arrays.asList(
				new Company( "Google" ),
				new Company( "Red Hat" ),
				new Company( "Microsoft" ) );
		List<Person> people = Arrays.asList(
				new Person( "BG", "Bill", "Gates" ),
				new Person( "LT", "Linus", "Torvalds" ),
				new Person( "SJ", "Steven", "Jobs" ) );

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		companies.forEach( c -> em.persist( c ) );
		people.forEach( p -> em.persist( p ) );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testMassIndexer() throws InterruptedException {

		// searches before mass index,
		// expected no results for each search
		List<Company> companies = findClass( Company.class, "name", "Google" );
		List<Person> people = findClass( Person.class, "firstName", "Linus" );
		assertEquals( 0, companies.size() );
		assertEquals( 0, people.size() );

		long executionId = indexCompany();
		JobExecution jobExecution = jobOperator.getJobExecution( executionId );
		jobExecution = keepTestAlive( jobExecution );
		List<StepExecution> stepExecutions = jobOperator.getStepExecutions( executionId );
		for ( StepExecution stepExecution : stepExecutions ) {
			logger.infof( "step %s executed.", stepExecution.getStepName() );
		}

		companies = findClass( Company.class, "name", "Google" );
		people = findClass( Person.class, "firstName", "Linus" );
		assertEquals( 1, companies.size() );
		assertEquals( 1, people.size() );
	}

	private <T> List<T> findClass(Class<T> clazz, String key, String value) {
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

	private long indexCompany() throws InterruptedException {
		// org.hibernate.search.jsr352.MassIndexer
		MassIndexer massIndexer = new MassIndexerImpl()
				.addRootEntities( Company.class, Person.class )
				.entityManager( emf.createEntityManager() )
				.jobOperator( jobOperator );
		long executionId = massIndexer.start();

		logger.infof( "job execution id = %d", executionId );
		return executionId;
	}

	public JobExecution keepTestAlive(JobExecution jobExecution) throws InterruptedException {
		int tries = 0;
		while ( !jobExecution.getBatchStatus().equals( BatchStatus.COMPLETED ) ) {
			if ( tries < JOB_MAX_TRIES ) {
				tries++;
				Thread.sleep( JOB_THREAD_SLEEP );
				jobExecution = jobOperator.getJobExecution( jobExecution.getExecutionId() );
			}
			else {
				break;
			}
		}
		return jobExecution;
	}

	private void testBatchStatus(StepExecution stepExecution) {
		BatchStatus batchStatus = stepExecution.getBatchStatus();
		switch ( stepExecution.getStepName() ) {

			case "loadId":
				long expectedEntityCount = DB_COMP_ROWS;
				// assertEquals(expectedEntityCount,
				// indexingContext.getEntityCount());
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
				testChunk( getMetricsMap( metrics ) );
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
		long companyCount = (long) Math.ceil( (double) DB_COMP_ROWS_LOADED / ARRAY_CAPACITY );
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
	public Map<Metric.MetricType, Long> getMetricsMap(Metric[] metrics) {
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
