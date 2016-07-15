/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hibernate.search.jsr352.MassIndexer;
import org.hibernate.search.jsr352.MassIndexerImpl;
import org.junit.Test;

/**
 *
 * @author Mincong Huang
 */
public class MassIndexerTest {

	private final boolean OPTIMIZE_AFTER_PURGE = true;
	private final boolean OPTIMIZE_AT_END = true;
	private final boolean PURGE_AT_START = true;
	private final int ARRAY_CAPACITY = 500;
	private final int FETCH_SIZE = 100000;
	private final int MAX_RESULTS = 1000000;
	private final int MAX_THREADS = 2;
	private final int PARTITION_CAPACITY = 500;

	/*
	 * Test if all params are correctly set
	 */
	@Test
	public void testJobParams() {

		MassIndexer massIndexer = new MassIndexerImpl()
				.arrayCapacity( ARRAY_CAPACITY )
				.fetchSize( FETCH_SIZE )
				.maxResults( MAX_RESULTS )
				.maxThreads( MAX_THREADS )
				.optimizeAfterPurge( OPTIMIZE_AFTER_PURGE )
				.optimizeAtEnd( OPTIMIZE_AT_END )
				.partitionCapacity( PARTITION_CAPACITY )
				.purgeAtStart( PURGE_AT_START );

		assertEquals( ARRAY_CAPACITY, massIndexer.getArrayCapacity() );
		assertEquals( FETCH_SIZE, massIndexer.getFetchSize() );
		assertEquals( MAX_RESULTS, massIndexer.getMaxResults() );
		assertEquals( OPTIMIZE_AFTER_PURGE, massIndexer.isOptimizeAfterPurge() );
		assertEquals( OPTIMIZE_AT_END, massIndexer.isOptimizeAtEnd() );
		assertEquals( PARTITION_CAPACITY, massIndexer.getPartitionCapacity() );
		assertEquals( PURGE_AT_START, massIndexer.isPurgeAtStart() );
		assertEquals( MAX_THREADS, massIndexer.getMaxThreads() );
	}

	/**
	 * Test if the set of root entities is set correctly via toString() method
	 */
	@Test
	public void testRootEntities_notNull() {

		MassIndexer massIndexer = new MassIndexerImpl()
				.addRootEntities( String.class, Integer.class );

		assertTrue( massIndexer.getRootEntities().contains( String.class ) );
		assertTrue( massIndexer.getRootEntities().contains( Integer.class ) );
	}

	@Test(expected = NullPointerException.class)
	public void testRootEntities_null() {
		new MassIndexerImpl().addRootEntities( null );
	}

	@Test(expected = IllegalStateException.class)
	public void testRootEntities_empty() {
		new MassIndexerImpl().addRootEntities( new Class<?>[0] );
	}
}
