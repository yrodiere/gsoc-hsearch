/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.util.Comparator;
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

		// create the 1st priority queue for partition units, comparable by rows
		PriorityQueue<_Unit> rowQueue = new PriorityQueue<>( partitions, new _RowComparator() );

		// compute rows to index for each entity and enqueue their properties
		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		int partitionCounter = 0;
		long totalEntityToIndex = 0;
		for ( String entityName : jobData.getEntityNameArray() ) {
			_Unit u = getPartitionUnit( entityName );
			totalEntityToIndex += u.rowsToIndex;
			logger.infof( "enqueue %s", u );
			rowQueue.add( u );
			partitionCounter++;
		}
		jobData.setTotalEntityToIndex( totalEntityToIndex );
		logger.infof( "totalEntityToIndex=%d", totalEntityToIndex );

		// enhance partitioning mechanism
		while ( partitionCounter < partitions ) {
			logger.info( "partitionCounter=" + partitionCounter );
			_Unit maxRowsU = rowQueue.poll();
			float half = maxRowsU.rowsToIndex / 2f;
			_Unit x = new _Unit( maxRowsU.entityName, (long) Math.floor( half ) );
			_Unit y = new _Unit( maxRowsU.entityName, (long) Math.ceil( half ) );
			rowQueue.add( x );
			rowQueue.add( y );
			rowQueue.forEach( u -> logger.info( u ) );
			partitionCounter++;
		}

		// create the 2nd priority queue for partition units, comparable by entity name
		PriorityQueue<_Unit> strQueue = new PriorityQueue<>( partitions, new _StringCompartor() );
		while ( !rowQueue.isEmpty() ) {
			strQueue.add( rowQueue.poll() );
		}
		Properties[] props = buildProperties(strQueue);

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

	private Properties[] buildProperties(PriorityQueue<_Unit> strQueue) {
		int i = 0;
		int partitions = strQueue.size();
		Properties[] props = new Properties[partitions];
		while ( !strQueue.isEmpty() && i < partitions ) {
			// each outer loop deals with one entity type (n partitions)
			// each inner loop deals with remainder problem for one entity type
			int remainder = 0;
			String prevEntityName = null;
			do {
				logger.infof( "inner loop: i=%d, remainder=%d, entityName=%s",
						i,
						remainder,
						strQueue.peek().entityName );
				_Unit u = strQueue.poll();
				props[i] = new Properties();
				props[i].setProperty( "entityName", u.entityName );
				props[i].setProperty( "scrollOffset", String.valueOf( remainder ) );
				prevEntityName = u.entityName;
				remainder++;
				i++;
			} while ( 0 < i && i < partitions
					&& !strQueue.isEmpty()
					&& strQueue.peek().entityName.equals( prevEntityName ));

			int interval = remainder;
			for ( int x = i - interval; x < i; x++ ) {
				logger.infof( "for loop: x=%d, i=%d, interval=%d", x, i, interval );
				props[x].setProperty( "scrollInterval", String.valueOf( interval ) );
			}
		}
		return props;
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
	private _Unit getPartitionUnit(String entityName)
			throws NamingException, HibernateException, ClassNotFoundException {

		EntityManager em = emf.createEntityManager();
		Session session = em.unwrap( Session.class );
		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		long rowCount = (long) session
				.createCriteria( jobData.getIndexedType( entityName ) )
				.setProjection( Projections.rowCount() )
				.setCacheable( false )
				.uniqueResult();

		em.close();
		logger.infof( "entityName=%s, rowCount=%d", entityName, rowCount );
		_Unit pp = new _Unit( entityName, rowCount );
		return pp;
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
			return  x.entityName.compareTo( y.entityName );
		}
	}
}
