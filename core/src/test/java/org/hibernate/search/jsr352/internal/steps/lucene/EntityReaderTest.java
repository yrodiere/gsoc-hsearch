/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashSet;

import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.search.jsr352.entity.Company;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.hibernate.search.jsr352.internal.se.JobSEEnvironment;
import org.hibernate.search.jsr352.internal.steps.lucene.EntityReader;
import org.hibernate.search.jsr352.internal.util.PartitionUnit;
import org.jboss.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Unit test for item reader validation.
 *
 * @author Mincong Huang
 */
public class EntityReaderTest {

	private static final Logger LOGGER = Logger.getLogger( EntityReaderTest.class );
	private final Company[] COMPANIES = new Company[]{
			new Company( "Red Hat" ),
			new Company( "Google" ),
			new Company( "Microsoft" ) };
	private EntityManagerFactory emf;

	@Mock
	private JobContext mockedJobContext;

	@Mock
	private StepContext mockedStepContext;

	@InjectMocks
	private EntityReader entityReader;

	@Before
	public void setUp() {
		EntityManager em = null;
		try {
			emf = Persistence.createEntityManagerFactory( "h2" );
			em = emf.createEntityManager();
			em.getTransaction().begin();
			for ( Company c : COMPANIES ) {
				em.persist( c );
			}
			em.getTransaction().commit();
		}
		finally {
			try {
				em.close();
			}
			catch (Exception e) {
				LOGGER.error( e );
			}
		}

		JobSEEnvironment.setEntityManagerFactory( emf );
		String cacheable = String.valueOf( false );
		String entityName = String.valueOf( Company.class );
		String fetchSize = String.valueOf( 1000 );
		String isJavaSE = String.valueOf( true );
		String maxResults = String.valueOf( Integer.MAX_VALUE );
		String partitionID = String.valueOf( 0 );
		entityReader = new EntityReader(cacheable,
				entityName,
				fetchSize,
				isJavaSE,
				maxResults,
				partitionID);

		MockitoAnnotations.initMocks( this );
	}

	@Test
	public void testReadItem_withoutBoundary() throws Exception {

		Object upper = null;
		Object lower = null;
		PartitionUnit partitionUnit = new PartitionUnit( Company.class, COMPANIES.length, lower, upper );

		// mock job context
		JobContextData jobData = new JobContextData();
		jobData.setCriterions( new HashSet<>() );
		jobData.setEntityClazzSet( new HashSet<>( Arrays.asList( Company.class ) ) );
		jobData.setPartitionUnits( Arrays.asList( partitionUnit ) );
		Mockito.when( mockedJobContext.getTransientUserData() ).thenReturn( jobData );

		// mock step context
		Mockito.doNothing().when( mockedStepContext ).setTransientUserData( Mockito.any() );

		entityReader.open( null );
		for ( int i = 0; i < COMPANIES.length; i++ ) {
			Company c = (Company) entityReader.readItem();
			assertEquals( COMPANIES[i].getName(), c.getName() );
		}
		// no more item
		assertNull( entityReader.readItem() );
	}
}
