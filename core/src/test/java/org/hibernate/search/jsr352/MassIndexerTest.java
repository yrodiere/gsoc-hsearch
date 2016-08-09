/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import javax.batch.operations.JobOperator;

import org.hibernate.search.jsr352.MassIndexerImpl;
import org.jboss.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 *
 * @author Mincong Huang
 */
@RunWith(MockitoJUnitRunner.class)
public class MassIndexerTest {

	private static final Logger LOGGER = Logger.getLogger( MassIndexerTest.class );
	private final boolean OPTIMIZE_AFTER_PURGE = true;
	private final boolean OPTIMIZE_AT_END = true;
	private final boolean PURGE_AT_START = true;
	private final int FETCH_SIZE = 100000;
	private final int MAX_RESULTS = 1000000;
	private final int MAX_THREADS = 2;
	private final int ROWS_PER_PARTITION = 500;

	@Mock
	private JobOperator mockedOperator;

	@Before
	public void setUp() {
		Mockito.when( mockedOperator.start( Mockito.anyString(), Mockito.any( Properties.class ) ) )
			.thenReturn( 1L );
	}

	@Test
	public void testJobParamsAll() {

		ArgumentCaptor<Properties> propsCaptor = ArgumentCaptor.forClass( Properties.class );
		long executionID = new MassIndexerImpl().jobOperator( mockedOperator )
				.addRootEntities( String.class, Integer.class )
				.fetchSize( FETCH_SIZE )
				.maxResults( MAX_RESULTS )
				.maxThreads( MAX_THREADS )
				.optimizeAfterPurge( OPTIMIZE_AFTER_PURGE )
				.optimizeAtEnd( OPTIMIZE_AT_END )
				.rowsPerPartition( ROWS_PER_PARTITION )
				.purgeAtStart( PURGE_AT_START )
				.start();
		assertEquals( 1L, executionID );

		Mockito.verify( mockedOperator )
				.start( Mockito.anyString(), propsCaptor.capture() );
		Properties props = propsCaptor.getAllValues().get( 0 );
		assertEquals( FETCH_SIZE, Integer.parseInt( props.getProperty( "fetchSize" ) ) );
		assertEquals( MAX_RESULTS, Integer.parseInt( props.getProperty( "maxResults" ) ) );
		assertEquals( OPTIMIZE_AFTER_PURGE, Boolean.parseBoolean( props.getProperty( "optimizeAfterPurge" ) ) );
		assertEquals( OPTIMIZE_AT_END, Boolean.parseBoolean( props.getProperty( "optimizeAtEnd" ) ) );
		assertEquals( ROWS_PER_PARTITION, Integer.parseInt( props.getProperty( "rowsPerPartition" ) ) );
		assertEquals( PURGE_AT_START, Boolean.parseBoolean( props.getProperty( "purgeAtStart" ) ) );
		assertEquals( MAX_THREADS, Integer.parseInt( props.getProperty( "maxThreads" ) ) );
		// TODO assert root entities
	}

	@Test
	public void testAddRootEntity_notNull() {
		MassIndexer massIndexer = new MassIndexerImpl().jobOperator( mockedOperator )
				.addRootEntity( Integer.class )
				.addRootEntity( String.class );
		// TODO captor values and assert
	}

	@Test(expected = NullPointerException.class)
	public void testAddRootEntity_null() {
		new MassIndexerImpl().addRootEntity( null );
	}

	@Test
	public void testAddRootEntities_notNull() {

		MassIndexer massIndexer = new MassIndexerImpl().jobOperator( mockedOperator )
				.addRootEntities( String.class, Integer.class );
		// TODO captor values and assert
	}

	@Test(expected = NullPointerException.class)
	public void testAddRootEntities_null() {
		new MassIndexerImpl().addRootEntities( null );
	}

	@Test(expected = IllegalStateException.class)
	public void testAddRootEntities_empty() {
		new MassIndexerImpl().addRootEntities( new Class<?>[0] );
	}
}
