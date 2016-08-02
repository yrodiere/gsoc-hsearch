package org.hibernate.search.jsr352;

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Projection;
import org.hibernate.search.jsr352.entity.Company;
import org.hibernate.search.jsr352.entity.Person;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.OngoingStubbing;

@RunWith(MockitoJUnitRunner.class)
public class PartitionMapperTest {

	private final int COMP_ROWS = 500;
	private final int PERS_ROWS = 10 * 1000;

	@Mock
	private SessionFactory mockedSessionFactory;

	@Mock
	private Session mockedSession;

	@Mock
	private StatelessSession mockedSS;

	@Mock
	private ScrollableResults mockedScroll;

	@Mock
	private Criteria mockedCompCriteria;

	@Mock
	private Criteria mockedPersCriteria;

	@Before
	public void setUp() {

		Mockito.when( mockedSessionFactory.openStatelessSession() )
				.thenReturn( mockedSS );

		// mock criteria for class Company
		Mockito.when( mockedSS.createCriteria( Company.class ) )
				.thenReturn( mockedCompCriteria );
		Mockito.when( mockedCompCriteria.setProjection( (Projection) Mockito.anyObject() ) )
				.thenReturn( mockedCompCriteria );
		Mockito.when( mockedCompCriteria.setReadOnly( Mockito.anyBoolean() ) )
				.thenReturn( mockedCompCriteria );
		Mockito.when( mockedCompCriteria.setFetchSize( Mockito.anyInt() ) )
				.thenReturn( mockedCompCriteria );
		Mockito.when( mockedCompCriteria.scroll( ScrollMode.FORWARD_ONLY ) )
				.thenReturn( mockedScroll );
		Mockito.when( mockedCompCriteria.uniqueResult() ).thenReturn( (Object) ( COMP_ROWS * 1L ) );
		OngoingStubbing<Object> scrollCompGet = Mockito.when( mockedScroll.get( 0 ) );
		OngoingStubbing<Boolean> scrollCompHasNext = Mockito.when( mockedScroll.next() );
		for ( int index = 0; index < PERS_ROWS; index++ ) {
			Company c = new Company();
			c.setId( index + 1 );
			c.setName( "Company " + ( index + 1 ) );
			scrollCompGet = scrollCompGet.thenReturn( c );
			scrollCompHasNext = scrollCompHasNext.thenReturn( true );
		}
		scrollCompHasNext = scrollCompHasNext.thenReturn( false );

		// mock criteria for class Person
		Mockito.when( mockedSS.createCriteria( Person.class ) )
				.thenReturn( mockedPersCriteria );
		Mockito.when( mockedPersCriteria.setProjection( (Projection) Mockito.anyObject() ) )
				.thenReturn( mockedPersCriteria );
		Mockito.when( mockedPersCriteria.setReadOnly( Mockito.anyBoolean() ) )
				.thenReturn( mockedPersCriteria );
		Mockito.when( mockedPersCriteria.setFetchSize( Mockito.anyInt() ) )
				.thenReturn( mockedPersCriteria );
		Mockito.when( mockedPersCriteria.scroll( ScrollMode.FORWARD_ONLY ) )
				.thenReturn( mockedScroll );
		Mockito.when( mockedPersCriteria.uniqueResult() ).thenReturn( (Object) ( PERS_ROWS * 1L ) );
		OngoingStubbing<Object> scrollPersGet = Mockito.when( mockedScroll.get( 0 ) );
		OngoingStubbing<Boolean> scrollPersHasNext = Mockito.when( mockedScroll.next() );
		for ( int index = 0; index < PERS_ROWS; index++ ) {
			Company c = new Company();
			c.setId( index + 1 );
			c.setName( "Person " + ( index + 1 ) );
			scrollPersGet = scrollPersGet.thenReturn( c );
			scrollPersHasNext = scrollPersHasNext.thenReturn( true );
		}
		scrollPersHasNext = scrollPersHasNext.thenReturn( false );
	}
}
