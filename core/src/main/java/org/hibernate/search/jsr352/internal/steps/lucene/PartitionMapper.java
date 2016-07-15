/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.util.Properties;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.jboss.logging.Logger;

/**
 * Lucene partition mapper provides a partition plan to the Lucene production
 * step: "produceLuceneDoc". The partition plan is defined dynamically,
 * according to the indexing context.
 * <p>
 * Several batch properties are used in this mapper:
 * <ul>
 * <li><b>partitionCapacity</b> defines the capacity of one partition: the
 * number of id arrays that will be treated in this partition. So the number of
 * partition is computed by the equation: <br>
 * {@code nbPartition = nbArray / partitionCapacity;}
 * <li><b>threads</b> defines the number of threads wished by the user. Default
 * value is defined in the job xml file. However, the valued used might be
 * smaller, depending on the number of partitions.
 * </ul>
 *
 * @author Mincong Huang
 */
@Named
public class PartitionMapper implements javax.batch.api.partition.PartitionMapper {

	private static final Logger logger = Logger.getLogger( PartitionMapper.class );

	private final JobContext jobContext;

	@Inject
	@BatchProperty
	private int partitionCapacity;

	@Inject
	@BatchProperty
	private int maxThreads;

	@Inject
	@BatchProperty
	private String persistenceUnitName;

	@Inject
	public PartitionMapper(JobContext jobContext) {
		this.jobContext = jobContext;
	}

	@Override
	public PartitionPlan mapPartitions() throws Exception {

		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		EntityMetadata[] metas = getEntityMetadatas( jobData.getEntityNameArray() );
		final int partitions = getPartitionCount( metas );

		Properties[] props = new Properties[partitions];
		for ( int i = 0; i < metas.length; i++ ) {
			for ( int j = i; j < i + metas[i].partitionCount; j++ ) {
				props[j] = new Properties();
				int remainder = j % metas[i].partitionCount;
				props[j].setProperty( "entityName", metas[i].entityName );
				props[j].setProperty( "scrollOffset", String.valueOf( remainder ) );
				props[j].setProperty( "scrollInterval", String.valueOf( metas[i].partitionCount ) );
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

	private int getPartitionCount(EntityMetadata[] metadatas) {
		int partitionCount = 0;
		for ( EntityMetadata meta : metadatas ) {
			partitionCount += meta.partitionCount;
		}
		return partitionCount;
	}

	/**
	 * Get an array of entity meta-data. This class is an inner class of
	 * PartitionMapper.
	 *
	 * @param entityNames an array of entity names
	 * @return an array of entity meta-data
	 * @throws NamingException if the target path is not found in the JNDI look
	 * up.
	 * @throws ClassNotFoundException if the entity type not found
	 * @throws HibernateException
	 */
	private EntityMetadata[] getEntityMetadatas(String[] entityNames)
			throws NamingException, HibernateException, ClassNotFoundException {

		EntityMetadata[] metas = new EntityMetadata[entityNames.length];
		String path = "java:comp/env/" + persistenceUnitName;
		EntityManager em = (EntityManager) InitialContext.doLookup( path );
		Session session = em.unwrap( Session.class );
		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();

		for ( int i = 0; i < entityNames.length; i++ ) {
			long rowCount = (long) session
					.createCriteria( jobData.getIndexedType( entityNames[i] ) )
					.setProjection( Projections.rowCount() )
					.setCacheable( false )
					.uniqueResult();
			logger.infof( "rowCount=%d, partitionCapacity=%d",
					rowCount,
					partitionCapacity );
			metas[i] = new EntityMetadata();
			metas[i].entityName = entityNames[i];
			metas[i].partitionCount = (int) Math.ceil( (double) rowCount / partitionCapacity );
		}
		return metas;
	}

	private class EntityMetadata {

		String entityName;
		int partitionCount;
	}
}
