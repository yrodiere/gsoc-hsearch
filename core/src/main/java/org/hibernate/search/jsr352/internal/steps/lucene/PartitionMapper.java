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

import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.hibernate.search.jsr352.internal.se.JobSEEnvironment;
import org.hibernate.search.jsr352.internal.util.MassIndexerUtil;
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

	private static final Logger LOGGER = Logger.getLogger( PartitionMapper.class );

	@Inject
	private JobContext jobContext;

	@Inject
	@BatchProperty
	private String fetchSize;

	@Inject
	@BatchProperty
	private String isJavaSE;

	@Inject
	@BatchProperty
	private String maxThreads;

	@Inject
	@BatchProperty
	private String rowsPerPartition;

	@PersistenceUnit(unitName = "h2")
	private EntityManagerFactory emf;

	PartitionMapper() {}

	/**
	 * Constructor for unit test.
	 * TODO should it be done in this way?
	 *
	 * @param fetchSize
	 * @param isJavaSE
	 * @param rowsPerPartition
	 */
	PartitionMapper(EntityManagerFactory emf,
			String fetchSize,
			String isJavaSE,
			String rowsPerPartition,
			String maxThreads) {
		this.emf = emf;
		this.fetchSize = fetchSize;
		this.isJavaSE = isJavaSE;
		this.maxThreads = maxThreads;
		this.rowsPerPartition = rowsPerPartition;
	}

	@Override
	public PartitionPlan mapPartitions() throws Exception {

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		SessionFactory sessionFactory = null;
		Session session = null;
		StatelessSession ss = null;
		ScrollableResults scroll = null;

		try {
			if ( Boolean.parseBoolean( isJavaSE ) ) {
				emf = JobSEEnvironment.getEntityManagerFactory();
			}
			sessionFactory = emf.unwrap( SessionFactory.class );
			session = sessionFactory.openSession();
			ss = sessionFactory.openStatelessSession();

			Set<Class<?>> rootEntities = jobData.getEntityClazzSet();
			List<PartitionUnit> partitionUnits = new ArrayList<>();

			for ( Class<?> clazz : rootEntities ) {
				setMonitor( clazz, session );
				String fieldID = MassIndexerUtil.getIdName( clazz, session );
				Criteria criteria = ss.createCriteria( clazz );
				jobData.getCriterions().forEach( c -> criteria.add( c ) );
				scroll = criteria.addOrder( Order.asc( fieldID ) )
						.setProjection( Projections.id() )
						.setFetchSize( Integer.parseInt( fetchSize ) )
						.setReadOnly( true )
						.scroll( ScrollMode.FORWARD_ONLY );
				Object lowerID = null;
				Object upperID = null;
				while ( scroll.scroll( Integer.parseInt( rowsPerPartition ) ) ) {
					lowerID = upperID;
					upperID = scroll.get( 0 );
					LOGGER.infof( "lowerID=%s", lowerID );
					LOGGER.infof( "upperID=%s", upperID );
					partitionUnits.add( new PartitionUnit( clazz,
							Integer.parseInt( rowsPerPartition ),
							lowerID,
							upperID ) );
				}
				// add an additional partition on the tail
				lowerID = upperID;
				upperID = null;
				LOGGER.infof( "lowerID=%s", lowerID );
				LOGGER.infof( "upperID=%s", upperID );
				partitionUnits.add( new PartitionUnit( clazz,
						Integer.parseInt( rowsPerPartition ),
						lowerID,
						upperID ) );
			}
			jobData.setPartitionUnits( partitionUnits );

			// Build partition plan
			final int threads = Integer.valueOf( maxThreads );
			final int partitions = partitionUnits.size();
			final Properties[] props = new Properties[partitions];
			LOGGER.infof( "%d partitions, %d threads.", partitions, threads );

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
				LOGGER.error( e );
			}
			try {
				ss.close();
			}
			catch (Exception e) {
				LOGGER.error( e );
			}
			try {
				session.close();
			}
			catch (Exception e) {
				LOGGER.error( e );
			}
		}
	}

	private void setMonitor(Class<?> clazz, Session session) {
		long rowCount = (long) session.createCriteria( clazz )
				.setProjection( Projections.rowCount() )
				.setCacheable( false )
				.uniqueResult();
		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		LOGGER.infof( "%d rows to index for entity type %s", rowCount, clazz.toString() );
		jobData.setRowsToIndex( clazz.toString(), rowCount );
		jobData.incrementTotalEntity( rowCount );
	}
}