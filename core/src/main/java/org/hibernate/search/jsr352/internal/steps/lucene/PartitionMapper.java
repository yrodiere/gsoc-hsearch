/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

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
	 * The maximal number of entities that can be processed inside a partition
	 */
	@Inject
	@BatchProperty
	private int partitionCapacity;

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
		StatelessSession ss = null;
		ScrollableResults scroll = null;

		try {

			sessionFactory = emf.unwrap( SessionFactory.class );
			session = sessionFactory.openSession();
			ss = sessionFactory.openStatelessSession();

			Set<Class<?>> rootEntities = jobData.getEntityClazzSet();
			List<PartitionUnit> partitionUnits = new ArrayList<>();

			for ( Class<?> clazz : rootEntities ) {
				setMonitor( clazz, session );
				final String fieldID = ContextHelper
						.getSearchintegrator( session )
						.getIndexBindings()
						.get( clazz )
						.getDocumentBuilder()
						.getIdentifierName();
				scroll = ss.createCriteria( clazz )
						.addOrder( Order.asc( fieldID ) )
						.setProjection( Projections.id() )
						.setCacheable( cacheable )
						.setFetchSize( fetchSize )
						.setReadOnly( true )
						.scroll( ScrollMode.FORWARD_ONLY );
				Object lowerID = null;
				Object upperID = null;
				while ( scroll.scroll( partitionCapacity ) ) {
					lowerID = upperID;
					upperID = scroll.get( 0 );
					partitionUnits.add( new PartitionUnit( clazz,
							partitionCapacity, lowerID, upperID ) );
				}
				// add an additional partition on the tail
				lowerID = upperID;
				upperID = null;
				partitionUnits.add( new PartitionUnit( clazz,
						partitionCapacity, lowerID, upperID ) );
			}
			jobData.setPartitionUnits( partitionUnits );

			// Build partition plan
			final int threads = maxThreads;
			final int partitions = partitionUnits.size();
			final Properties[] props = new Properties[partitions];
			logger.infof( "%d partitions, %d threads.", partitions, threads );

			for ( int i = 0; i < partitionUnits.size(); i++ ) {
				props[i] = new Properties();
				props[i].setProperty( "entityName", partitionUnits.get( i ).getEntityName() );
				props[i].setProperty( "partitionID", String.valueOf( i ) );
			}

			PartitionPlan partitionPlan = new PartitionPlanImpl();
			partitionPlan.setPartitionProperties( props );
			partitionPlan.setPartitions( partitions );
			partitionPlan.setThreads( threads );
			return partitionPlan;
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
			try {
				session.close();
			}
			catch (Exception e) {
				logger.error( e );
			}
		}
	}

	private void setMonitor( Class<?> clazz, Session session ) {
		long rowCount = (long) session.createCriteria( clazz )
				.setProjection( Projections.rowCount() )
				.setCacheable( false )
				.uniqueResult();
		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		jobData.setRowsToIndex( clazz.toString(), rowCount );
		jobData.incrementTotalEntity( rowCount );
	}
}
