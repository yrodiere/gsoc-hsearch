/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Properties;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.jboss.logging.Logger;

/**
 * Lucene partition mapper provides a partition plan to the Lucene production
 * step: "produceLuceneDoc". The partition plan is defined dynamically,
 * according to the partition capacity.
 *
 * @author Mincong Huang
 */
@Named
public class PartitionMapper implements javax.batch.api.partition.PartitionMapper {

	private static final Logger logger = Logger.getLogger( PartitionMapper.class );

	private final JobContext jobContext;

	@PersistenceUnit(unitName = "h2")
	private EntityManagerFactory emf;

	/**
	 * The partition-capacity defines the max number of entities to be processed
	 * inside a partition. So the number of partitions used will be the division
	 * of the entities to index and the partition capacity :
	 * {@code partitions = entitiesToDo / partitionCapacity;}
	 */
//	@Inject
//	@BatchProperty
//	private int partitionCapacity;

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

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		List<_EntityMetadata> metas = getEntityMetadatas( jobData.getEntityNameArray() );

		Comparator<_PartitionProperty> byRowDesc =
				((_PartitionProperty x, _PartitionProperty y) -> (int)( y.rowsToDo - x.rowsToDo ));
		PriorityQueue<_PartitionProperty> rowQueue = new PriorityQueue<>( metas.size(), byRowDesc );

		for ( _EntityMetadata m : metas ) {
			rowQueue.add( new _PartitionProperty( m.entityName, m.rowCount ) );
		}

		for ( int i = 0; i < partitions; i++ ) {
			_PartitionProperty maxRowsPP = rowQueue.poll();
			String entityName = maxRowsPP.entityName;
			double half = maxRowsPP.rowsToDo / 2f;
			_PartitionProperty x = new _PartitionProperty( entityName, (long) Math.floor( half ) );
			_PartitionProperty y = new _PartitionProperty( entityName, (long) Math.ceil( half ) );
			rowQueue.add( x );
			rowQueue.add( y );
			logger.info( "i=" + i );
			rowQueue.forEach( e -> logger.info( e.entityName + " " + e.rowsToDo ) );
		}

		Comparator<_PartitionProperty> byEntityName =
				((_PartitionProperty x, _PartitionProperty y) -> x.entityName.compareTo( y.entityName ));
		PriorityQueue<_PartitionProperty> strQueue = new PriorityQueue<>( rowQueue.size(), byEntityName );
		while( !rowQueue.isEmpty() ) {
			_PartitionProperty pp = rowQueue.poll();
			strQueue.add( pp );
		}

		int j = 0;
		Properties[] props = new Properties[partitions];
		logger.infof( "strQueue.size=%d", strQueue.size() );
		while ( !strQueue.isEmpty() && j < partitions ) {
			// each outer loop deals with one entity type (n partitions)
			// each inner loop deals with remainder problem for one entity type
			int remainder = 0;
			while ( j < partitions && ((j > 0 && strQueue.peek().entityName.equals( props[j - 1].getProperty( "entityName" ) ))
					|| j == 0)) {
				logger.infof( "inner loop: j = %d, remainder = %d, entityName = %s", j, remainder, strQueue.peek().entityName );
				props[j] = new Properties();
				_PartitionProperty pp = strQueue.poll();
				props[j].setProperty( "entityName", pp.entityName );
				props[j].setProperty( "scrollOffset", String.valueOf( remainder ) );
				remainder++;
				j++;
			}
			int interval = remainder;
			for (int i = j - interval; i < j; i++ ) {
				logger.infof( "for loop: i=%d, j=%d, interval=%d", i, j, interval );
				props[i].setProperty( "scrollInterval", String.valueOf( interval ) );
			}
		}

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

	/**
	 * Get a list of entity meta-data. This class is an inner class of
	 * PartitionMapper.
	 *
	 * @param entityNames an array of entity names
	 * @return a list of entity meta-data
	 * @throws NamingException if the target path is not found in the JNDI look
	 * up.
	 * @throws ClassNotFoundException if the entity type not found
	 * @throws HibernateException
	 */
	private List<_EntityMetadata> getEntityMetadatas(String[] entityNames)
			throws NamingException, HibernateException, ClassNotFoundException {

		List<_EntityMetadata> metas = new ArrayList<>();
		EntityManager em = emf.createEntityManager();
		Session session = em.unwrap( Session.class );
		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		long totalEntityToIndex = 0;

		for ( String entityName : entityNames ) {
			long rowCount = (long) session
					.createCriteria( jobData.getIndexedType( entityName ) )
					.setProjection( Projections.rowCount() )
					.setCacheable( false )
					.uniqueResult();
			logger.infof( "entityName=%s, rowCount=%d, partitions=%d",
					entityName,
					rowCount,
					partitions );
			_EntityMetadata m = new _EntityMetadata();
			m.entityName = entityName;
			m.rowCount = rowCount;
			metas.add( m );
			totalEntityToIndex += rowCount;
		}

		jobData.setTotalEntityToIndex( totalEntityToIndex );
		logger.infof( "totalEntityToIndex=%d", totalEntityToIndex );
		em.close();
		return metas;
	}

	private class _EntityMetadata {

		String entityName;
		long rowCount;
	}

	private class _PartitionProperty {

		String entityName;
		long rowsToDo;

		_PartitionProperty(String entityName, long rowsToDo) {
			this.entityName = entityName;
			this.rowsToDo = rowsToDo;
		}
	}
}
