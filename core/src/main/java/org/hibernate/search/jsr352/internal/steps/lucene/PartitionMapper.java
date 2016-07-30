/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.util.Arrays;
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
import org.hibernate.search.jsr352.internal.util.PartitionUnit;
import org.hibernate.search.jsr352.internal.util.RowComparator;
import org.hibernate.search.jsr352.internal.util.StringComparator;
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
	@BatchProperty(name = "partitions")
	private int initialPartitions;
	private int finalPartitions;

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

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		SessionFactory sessionFactory = null;
		Session session = null;

		try {

			sessionFactory = emf.unwrap( SessionFactory.class );
			session = sessionFactory.openSession();
			int tailPartitions = jobData.getEntityNameArray().length;
			finalPartitions = initialPartitions * maxThreads + tailPartitions;

			// Use priority queue to order partition units by rows.
			PriorityQueue<PartitionUnit> rowQueue = new PriorityQueue<>(
					initialPartitions * maxThreads,
					new RowComparator() );
			int partitions = 0;
			for ( String entityName : jobData.getEntityNameArray() ) {
				PartitionUnit u = buildPartitionUnit( entityName, session );
				jobData.setRowsToIndex( u.getEntityName(), (int) u.getRowsToIndex() );
				jobData.incrementTotalEntity( (int) u.getRowsToIndex() );
				rowQueue.add( u );
				partitions++;
			}

			// Enhance partitioning mechanism
			while ( partitions < initialPartitions * maxThreads ) {
				PartitionUnit maxRowsU = rowQueue.poll();
				Class<?> clazz = maxRowsU.getEntityClazz();
				float half = maxRowsU.getRowsToIndex() / 2f;
				PartitionUnit x = new PartitionUnit( clazz, (long) Math.floor( half ) );
				PartitionUnit y = new PartitionUnit( clazz, (long) Math.ceil( half ) );
				rowQueue.add( x );
				rowQueue.add( y );
				partitions++;
			}
			rowQueue.forEach( u -> logger.info( u ) );

			// Use another priority queue to reorder partition units by entity name
			PriorityQueue<PartitionUnit> strQueue = new PriorityQueue<>(
					initialPartitions * maxThreads,
					new StringComparator() );
			while ( !rowQueue.isEmpty() ) {
				strQueue.add( rowQueue.poll() );
			}

			// Build partition plan
			final Properties[] props = buildProperties( strQueue, session );
			final int threads = maxThreads;
			logger.infof( "%d partitions, %d threads.", initialPartitions, threads );
			PartitionPlan partitionPlan = new PartitionPlanImpl();
			partitionPlan.setPartitionProperties( props );
			partitionPlan.setPartitions( finalPartitions );
			partitionPlan.setThreads( threads );
			return partitionPlan;
		}
		finally {
			try {
				session.close();
			}
			catch (Exception e) {
				logger.error( e );
			}
		}
	}

	/**
	 * Build a property array using a partition unit priority queue ordered by
	 * entity name.
	 *
	 * @param strQueue string-comparable queue
	 * @param session Hibernate session
	 * @return a property array
	 * @throws ClassNotFoundException if target entity class type is not found
	 * during the creation of the scrollable result
	 * @throws HibernateException if any hibernate occurs during the creation of
	 * scrollable results
	 */
	private Properties[] buildProperties(PriorityQueue<PartitionUnit> strQueue,
			Session session) throws HibernateException, ClassNotFoundException {

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		StatelessSession ss = session.getSessionFactory().openStatelessSession();
		ScrollableResults scroll = null;

		try {
			int partitionID = 0;
			Properties[] props = new Properties[finalPartitions];
			PartitionUnit[] units = new PartitionUnit[finalPartitions];
			while ( !strQueue.isEmpty() && partitionID < finalPartitions ) {
				// each outer loop handles one entity type (N partitions)
				// each inner loop handles one partition
				int partitionCounter = 0;
				String entityName = null;

				do {
					logger.infof( "inner loop: partitionID=%d, partitionCounter=%d, entityName=%s",
							partitionID,
							partitionCounter,
							strQueue.peek().getEntityName() );
					PartitionUnit u = strQueue.poll();
					props[partitionID] = new Properties();
					props[partitionID].setProperty( "entityName", u.getEntityName() );
					props[partitionID].setProperty( "partitionID", String.valueOf( partitionID ) );
					entityName = u.getEntityName();
					partitionCounter++;
					partitionID++;

				} while ( partitionID < finalPartitions &&
						!strQueue.isEmpty() &&
						strQueue.peek().getEntityName().equals( entityName ) );

				final long rows = jobData.getRowsToIndex( entityName );
				final int partitionCapacity = (int) ( rows / partitionCounter );
				final String fieldID = ContextHelper
						.getSearchintegrator( session )
						.getIndexBindings()
						.get( jobData.getIndexedType( entityName ) )
						.getDocumentBuilder()
						.getIdentifierName();

				// Add an additional partition for entities inserted after the
				// start of the job
				props[partitionID] = new Properties();
				props[partitionID].setProperty( "entityName", entityName );
				props[partitionID].setProperty( "partitionID", String.valueOf( partitionID ) );
				partitionID++;

				Class<?> entityClazz = jobData.getIndexedType( entityName );
				scroll = ss.createCriteria( entityClazz )
						.addOrder( Order.asc( fieldID ) )
						.setProjection( Projections.id() )
						.setCacheable( cacheable )
						.setFetchSize( fetchSize )
						.setReadOnly( true )
						.scroll( ScrollMode.FORWARD_ONLY );

				int x = partitionID - 1 - partitionCounter;
				Object lowerID = null;
				Object upperID = null;
				while ( x < partitionID ) {
					// swift boundary
					if ( scroll.scroll( partitionCapacity ) ) {
						lowerID = upperID;
						upperID = scroll.get( 0 );
					}
					else {
						lowerID = upperID;
						upperID = null;
					}

					units[x] = new PartitionUnit(
							entityClazz,
							partitionCapacity,
							lowerID,
							upperID );
					x++;
				}
			}
			jobData.setPartitionUnits( units );
			logger.info( Arrays.toString( units ) );
			return props;
		}
		finally {
			try {
				scroll.close();
			}
			catch (Exception e) {
				logger.error( e );
			}
			try {
				ss.close();
			}
			catch (Exception e) {
				logger.error( e );
			}
		}
	}

	/**
	 * Build initial partition unit using the entity name. Partition unit is an
	 * inner class of PartitionMapper.
	 *
	 * @param entityName entity name
	 * @param session Hibernate session unwrapped from entity manager
	 * @return initial partition unit, it can be enhanced by other method and
	 * become a final partition unit.
	 * @throws ClassNotFoundException if the entity type not found
	 * @throws HibernateException
	 */
	private PartitionUnit buildPartitionUnit(String entityName, Session session)
			throws HibernateException, ClassNotFoundException {

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		Class<?> entityClazz = jobData.getIndexedType( entityName );
		long rowCount = (long) session
				.createCriteria( entityClazz )
				.setProjection( Projections.rowCount() )
				.setCacheable( false )
				.uniqueResult();
		PartitionUnit u = new PartitionUnit( entityClazz, rowCount );
		logger.info( u );
		return u;
	}
}
