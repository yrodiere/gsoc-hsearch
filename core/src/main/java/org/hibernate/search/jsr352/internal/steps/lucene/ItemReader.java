/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.io.Serializable;

import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
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
import org.hibernate.criterion.Restrictions;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.hibernate.search.jsr352.internal.util.PartitionBoundary;
import org.jboss.logging.Logger;

/**
 * TODO: update description. Read entity IDs from {@code IndexingContext}. Each
 * time, there's one array being read. The number of IDs inside the array
 * depends on the array capacity. This value is defined before the job start.
 * Either the default value defined in the job xml will be applied, or the value
 * overwritten by the user in job parameters. These IDs will be processed in
 * {@code BatchItemProcessor}, then be used for Lucene document production.
 * <p>
 * The motivation of using an array of IDs over a single ID is to accelerate the
 * entity processing. Use a SELECT statement to obtain only one ID is rather a
 * waste. For more detail about the entity process, please check {@code
 * BatchItemProcessor}.
 *
 * @author Mincong Huang
 */
@Named
public class ItemReader implements javax.batch.api.chunk.ItemReader {

	private static final Logger logger = Logger.getLogger( ItemReader.class );

	@PersistenceUnit(unitName = "h2")
	private EntityManagerFactory emf;

	@Inject
	@BatchProperty
	private boolean cacheable;

	@Inject
	@BatchProperty
	private int fetchSize;

	@Inject
	@BatchProperty
	private int maxResults;

	@Inject
	@BatchProperty
	private int partitionIndex;

	@Inject
	@BatchProperty
	private String entityName;

	private Class<?> entityClazz;
	private Serializable checkpointID;
	private final JobContext jobContext;
	private final StepContext stepContext;

	// read entities and produce Lucene work
	private Session session;
	private StatelessSession ss;
	private ScrollableResults scroll;
	private SessionFactory sessionFactory;

	@Inject
	public ItemReader(JobContext jobContext, StepContext stepContext) {
		this.jobContext = jobContext;
		this.stepContext = stepContext;
	}

	/**
	 * The checkpointInfo method returns the current checkpoint data for this
	 * reader. It is called before a chunk checkpoint is committed.
	 *
	 * @return the checkpoint info
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public Serializable checkpointInfo() throws Exception {
		logger.info( "checkpointInfo() called. "
				+ "Saving last read ID to batch runtime..." );
		return checkpointID;
	}

	/**
	 * Close operation(s) before the class destruction.
	 *
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public void close() throws Exception {
		logger.info( "closing everything..." );
		try {
			scroll.close();
			logger.info( "Scrollable results closed." );
		}
		catch (Exception e) {
			logger.error( e );
		}
		try {
			ss.close();
			logger.info( "Stateless session closed." );
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
		// reset the chunk work count to avoid over-count in item collector
		// release session
		StepContextData stepData = (StepContextData) stepContext.getTransientUserData();
		stepData.setChunkWorkCount( 0 );
		stepData.setSession( null );
		stepContext.setPersistentUserData( stepData );
	}

	/**
	 * Initialize the environment. If checkpoint does not exist, then it should
	 * be the first open. If checkpoint exists, then it isn't the first open,
	 * re-use the input object "checkpoint" as the last ID already read.
	 *
	 * @param checkpoint The last checkpoint info persisted in the batch
	 * runtime, previously given by checkpointInfo(). If this is the first
	 * start, then the checkpoint will be null.
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public void open(Serializable checkpointID) throws Exception {

		logger.infof( "open reader for entity %s ...", entityName );
		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		entityClazz = jobData.getIndexedType( entityName );
		PartitionBoundary boundary = jobData.getPartitionBoundary( partitionIndex );
		logger.info( boundary );

		sessionFactory = emf.unwrap( SessionFactory.class );
		ss = sessionFactory.openStatelessSession();
		session = sessionFactory.openSession();
		scroll = buildScroll( ss, session, boundary, checkpointID );

		StepContextData stepData = null;
		if ( checkpointID == null ) {
			stepData = new StepContextData();
		}
		else {
			stepData = (StepContextData) stepContext.getPersistentUserData();
		}

		stepData.setSession( session );
		stepContext.setTransientUserData( stepData );
	}

	private ScrollableResults buildScroll(StatelessSession ss, Session session,
			PartitionBoundary boundary, Object checkpointID) {

		String idName = ContextHelper
				.getSearchintegrator( session )
				.getIndexBindings()
				.get( entityClazz )
				.getDocumentBuilder()
				.getIdentifierName();
		Criteria criteria = ss.createCriteria( entityClazz );
		if ( checkpointID != null ) {
			criteria.add( Restrictions.ge( idName, checkpointID ) );
		}

		if ( boundary.isUniquePartition() ) {
			// do nothing
		}
		else if ( boundary.isFirstPartition() ) {
			criteria.add( Restrictions.le( idName, boundary.getUpperID() ) );
		}
		else if ( boundary.isLastPartition() ) {
			criteria.add( Restrictions.ge( idName, boundary.getLowerID() ) );
		}
		else {
			criteria.add( Restrictions.ge( idName, boundary.getLowerID() ) )
					.add( Restrictions.le( idName, boundary.getUpperID() ) );
		}
		return criteria.addOrder( Order.asc( idName ) )
				.setReadOnly( true )
				.setCacheable( cacheable )
				.setFetchSize( fetchSize )
				.setMaxResults( maxResults )
				.scroll( ScrollMode.FORWARD_ONLY );
	}

	/**
	 * Read item from database using JPA. Each read, there will be only one
	 * entity fetched.
	 *
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public Object readItem() throws Exception {
		logger.debug( "Reading item ..." );
		Object entity = null;

		if ( scroll.next() ) {
			entity = scroll.get( 0 );
			checkpointID = (Serializable) emf.getPersistenceUnitUtil()
					.getIdentifier( entity );
		}
		else {
			logger.info( "no more result. read ends." );
		}
		return entity;
	}
}
