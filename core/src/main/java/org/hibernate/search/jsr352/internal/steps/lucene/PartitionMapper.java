/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Properties;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.hibernate.search.jsr352.internal.util.PartitionBoundary;
import org.jboss.logging.Logger;

/**
 * Lucene partition mapper provides a partition plan to the Lucene production
 * step: "produceLuceneDoc". The partition plan is defined dynamically,
 * according to the number of partitions given by the user.
 *
 * @author Mincong Huang
 */
@Named
public class PartitionMapper implements javax.batch.api.partition.PartitionMapper {

	private static final Logger logger = Logger.getLogger( PartitionMapper.class );
	private final JobContext jobContext;

	@PersistenceUnit(unitName = "h2")
	private EntityManagerFactory emf;

	@Inject
	@BatchProperty
	private boolean cacheable;

	@Inject
	@BatchProperty
	private int fetchSize;

	/**
	 * The number of partitions used for this partitioned chunk.
	 */
	@Inject
	@BatchProperty
	private int partitions;

	/**
	 * The max number of threads used by the job
	 */
	@Inject
	@BatchProperty
	private int maxThreads;

	@Inject
	public PartitionMapper(JobContext jobContext) {
		this.jobContext = jobContext;
	}

	@Override
	public PartitionPlan mapPartitions() throws Exception {

		SessionFactory sessionFactory = null;
		Session session = null;
		try {
			sessionFactory = emf.unwrap( SessionFactory.class );
			session = sessionFactory.openSession();

			// Create the 1st priority queue for partition units, order by rows.
			// Then construct these units and enqueue them.
			PriorityQueue<_Unit> rowQueue = new PriorityQueue<>( partitions, new _RowComparator() );
			JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
			for ( String entityName : jobData.getEntityNameArray() ) {
				_Unit u = getPartitionUnit( entityName, session );
				jobData.setRowsToIndex( u.entityName, u.rowsToIndex );
				jobData.incrementTotalEntity( u.rowsToIndex );
				logger.infof( "enqueue %s", u );
				rowQueue.add( u );
			}

			// Enhance partitioning mechanism
			int partitionCounter = jobData.getEntityNameArray().length;
			while ( partitionCounter < partitions ) {
				logger.infof( "partitionCounter=%d", partitionCounter );
				_Unit maxRowsU = rowQueue.poll();
				float half = maxRowsU.rowsToIndex / 2f;
				_Unit x = new _Unit( maxRowsU.entityName, (long) Math.floor( half ) );
				_Unit y = new _Unit( maxRowsU.entityName, (long) Math.ceil( half ) );
				rowQueue.add( x );
				rowQueue.add( y );
				partitionCounter++;
			}
			rowQueue.forEach( u -> logger.info( u ) );

			// Create the 2nd priority queue to reorder these partition units, order
			// by entity name
			PriorityQueue<_Unit> strQueue = new PriorityQueue<>( partitions, new _StringCompartor() );
			while ( !rowQueue.isEmpty() ) {
				strQueue.add( rowQueue.poll() );
			}
			Properties[] props = buildProperties( strQueue, session );

			return new PartitionPlanImpl() {

				@Override
				public int getPartitions() {
					logger.infof( "#mapPartitions(): %d partitions.", partitions );
					return partitions;
				}

				@Override
				public int getThreads() {
					int threads = Math.min( maxThreads, partitions );
					logger.infof( "#getThreads(): %d threads.", threads );
					return threads;
				}

				@Override
				public Properties[] getPartitionProperties() {
					return props;
				}
			};
		}
		finally {
			try {
				session.close();
			}
			catch ( Exception e ) {
				logger.error( e );
			}
		}
	}

	/**
	 * Build a property array using string-comparable queue, which means units
	 * are ordered by entity name. So units having the same entity name are
	 * placed next to each other.
	 *
	 * @param strQueue string-comparable queue
	 * @param session Hibernate session
	 * @return a property array
	 * @throws ClassNotFoundException if target entity class type is not found
	 * during the creation of the scrollable result
	 * @throws HibernateException if any hibernate occurs during the creation of
	 * scrollable results
	 */
	private Properties[] buildProperties(PriorityQueue<_Unit> strQueue, Session session)
			throws HibernateException, ClassNotFoundException {

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		StatelessSession ss = session.getSessionFactory().openStatelessSession();
		ScrollableResults scroll = null;
		int i = 0;
		Properties[] props = new Properties[partitions];
		PartitionBoundary[] boundaries = new PartitionBoundary[partitions];

		try {
			while ( !strQueue.isEmpty() && i < partitions ) {
				// each outer loop deals with one entity type (n partitions)
				// each inner loop deals with remainder problem for one entity type
				int partitionCounter = 0;
				String currEntityName = null;

				do {
					logger.infof( "inner loop: i=%d, partitionCounter=%d, entityName=%s",
							i,
							partitionCounter,
							strQueue.peek().entityName );
					_Unit u = strQueue.poll();
					props[i] = new Properties();
					props[i].setProperty( "entityName", u.entityName );
					props[i].setProperty( "partitionIndex", String.valueOf( i ) );
					currEntityName = u.entityName;
					partitionCounter++;
					i++;
				} while ( i < partitions
						&& !strQueue.isEmpty()
						&& strQueue.peek().entityName.equals( currEntityName ) );

				final long rows = jobData.getRowsToIndex( currEntityName );
				final int partitionCapacity = (int) ( rows / partitionCounter );
				final String fieldID = ContextHelper
						.getSearchintegrator( session )
						.getIndexBindings()
						.get( jobData.getIndexedType( currEntityName ) )
						.getDocumentBuilder()
						.getIdentifierName();
				scroll = ss.createCriteria( jobData.getIndexedType( currEntityName ) )
						.addOrder( Order.asc( fieldID ) )
						.setProjection( Projections.id() )
						.setCacheable( cacheable )
						.setFetchSize( fetchSize )
						.setReadOnly( true )
						.scroll( ScrollMode.FORWARD_ONLY );

				int x = i - partitionCounter;
				Object lowerID = null;
				Object upperID = null;
				while ( x < i ) {
					if ( scroll.scroll( partitionCapacity ) ) {
						lowerID = upperID;
						upperID = scroll.get( 0 );
						boundaries[x] = new PartitionBoundary( lowerID, upperID );
					}
					else {
						lowerID = upperID;
						boundaries[x] = new PartitionBoundary( lowerID, null );
					}
					x++;
				}
			}
			jobData.setPartitionBoundaries( boundaries );
			logger.info( Arrays.toString( boundaries ) );
		}
		finally {
			try {
				scroll.close();
			}
			catch ( Exception e ) {
				logger.error( e );
			}
			try {
				ss.close();
			}
			catch ( Exception e ) {
				logger.error( e );
			}
		}
		return props;
	}

	/**
	 * Get initial partition unit using the entity name. Partition unit is an
	 * inner class of PartitionMapper.
	 *
	 * @param entityName entity name
	 * @param session Hibernate session unwrapped from entity manager
	 * @return initial partition unit, it can be enhanced by other method and
	 * become a final partition unit.
	 * @throws ClassNotFoundException if the entity type not found
	 * @throws HibernateException
	 */
	private _Unit getPartitionUnit(String entityName, Session session)
			throws HibernateException, ClassNotFoundException {

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		long rowCount = (long) session
				.createCriteria( jobData.getIndexedType( entityName ) )
				.setProjection( Projections.rowCount() )
				.setCacheable( false )
				.uniqueResult();
		_Unit u = new _Unit( entityName, rowCount );
		logger.info( u );
		return u;
	}

	private class _Unit {

		String entityName;
		long rowsToIndex;

		_Unit(String entityName, long rowsToIndex) {
			this.entityName = entityName;
			this.rowsToIndex = rowsToIndex;
		}

		@Override
		public String toString() {
			return "_Unit [entityName=" + entityName + ", rowsToIndex=" + rowsToIndex + "]";
		}
	}

	private class _RowComparator implements Comparator<_Unit> {

		@Override
		public int compare(_Unit x, _Unit y) {
			if ( x.rowsToIndex < y.rowsToIndex ) {
				return 1;
			}
			if ( x.rowsToIndex > y.rowsToIndex ) {
				return -1;
			}
			return 0;
		}
	}

	private class _StringCompartor implements Comparator<_Unit> {

		@Override
		public int compare(_Unit x, _Unit y) {
			return x.entityName.compareTo( y.entityName );
		}
	}
}
