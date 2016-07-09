/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.util.Properties;
import java.util.Set;

import javax.batch.api.BatchProperty;
import javax.batch.api.partition.PartitionPlan;
import javax.batch.api.partition.PartitionPlanImpl;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.search.jsr352.internal.BatchContextData;
import org.hibernate.search.jsr352.internal.IndexingContext;
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
 * @author Mincong HUANG
 */
@Named
public class PartitionMapper implements javax.batch.api.partition.PartitionMapper {

	private static final Logger logger = Logger.getLogger( PartitionMapper.class );

	private final JobContext jobContext;
	private final IndexingContext indexingContext;

	@Inject
	@BatchProperty
	private int partitionCapacity;
	@Inject
	@BatchProperty
	private int threads;

	@Inject
	public PartitionMapper(JobContext jobContext, IndexingContext indexingContext) {
		this.jobContext = jobContext;
		this.indexingContext = indexingContext;
	}

	@Override
	public PartitionPlan mapPartitions() throws Exception {

		Set<Class<?>> rootEntityClazzes = ( (BatchContextData) jobContext.getTransientUserData() )
				.getEntityTypesToIndex();
		final int TOTAL_PARTITIONS = rootEntityClazzes.size();

		return new PartitionPlanImpl() {

			@Override
			public int getPartitions() {
				logger.infof( "#mapPartitions(): %d partitions.", TOTAL_PARTITIONS );
				return TOTAL_PARTITIONS;
			}

			@Override
			public int getThreads() {
				logger.infof( "#getThreads(): %d threads.", TOTAL_PARTITIONS );// Math.min(TOTAL_PARTITIONS,
																				// threads));
				return Math.min( TOTAL_PARTITIONS, threads );
			}

			@Override
			public Properties[] getPartitionProperties() {
				Properties[] props = new Properties[TOTAL_PARTITIONS];
				final Class<?>[] rootEntityArray = rootEntityClazzes
						.toArray( new Class<?>[rootEntityClazzes.size()] );
				for ( int i = 0; i < props.length; i++ ) {
					String entityName = rootEntityArray[i].getName();
					props[i] = new Properties();
					props[i].setProperty( "entityName", entityName );
				}
				return props;
			}
		};
	}
}
