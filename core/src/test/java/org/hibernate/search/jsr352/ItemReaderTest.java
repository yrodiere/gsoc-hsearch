/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.batch.api.partition.PartitionPlan;
import javax.batch.runtime.context.JobContext;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.jsr352.entity.Company;
import org.hibernate.search.jsr352.entity.Person;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.hibernate.search.jsr352.internal.steps.lucene.ItemReader;
import org.hibernate.search.jsr352.internal.steps.lucene.PartitionMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for item reader validation.
 *
 * @author Mincong Huang
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ContextHelper.class)
public class ItemReaderTest {

	private final Company[] COMPANIES = new Company[]{
			new Company( "Red Hat" ),
			new Company( "Google" ),
			new Company( "Microsoft" ) };

	@Mock
	private ScrollableResults scroll;

	@Mock
	private EntityManagerFactory emf;

	@Mock
	private PersistenceUnitUtil puUtil;

	@InjectMocks
	private ItemReader itemReader;

	@Before
	public void setUp() {
		Mockito.when( scroll.next() )
				.thenReturn( true )
				.thenReturn( true )
				.thenReturn( true )
				.thenReturn( false );
		Mockito.when( scroll.get( Mockito.anyInt() ) )
				.thenReturn( COMPANIES[0] )
				.thenReturn( COMPANIES[1] )
				.thenReturn( COMPANIES[2] );
		Mockito.when( emf.getPersistenceUnitUtil() ).thenReturn( puUtil );
		Mockito.when( puUtil.getIdentifier( Mockito.anyObject() ) ).thenReturn( "id" );
	}

	@Test
	public void testReadItem() throws Exception {

		for ( int i = 0; i < COMPANIES.length; i++ ) {
			Company realCompany = (Company) itemReader.readItem();
			assertEquals( COMPANIES[i], realCompany );
		}
		Object lastItem = itemReader.readItem();
		assertNull( lastItem );
	}
}
