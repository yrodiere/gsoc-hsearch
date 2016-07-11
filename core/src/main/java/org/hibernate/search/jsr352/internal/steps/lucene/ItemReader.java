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
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.InitialContext;
import javax.naming.NoInitialContextException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.impl.HibernateSessionLoadingInitializer;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.hibernate.search.spi.InstanceInitializer;
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
 * @author Mincong HUANG
 */
@Named
public class ItemReader implements javax.batch.api.chunk.ItemReader {

	private static final Logger logger = Logger.getLogger( ItemReader.class );

	@Inject
	@BatchProperty
	private String entityName;

	@Inject
	@BatchProperty
	private int maxResults;

	@Inject
	@BatchProperty
	private String persistenceUnitName;

	private Class<?> entityClazz;
	private JobContext jobContext;
	private Serializable checkpointId;

	// read entities and produce Lucene work
	private EntityManagerFactory emf = null;
	private EntityManager em;
	private Session session;
	private StatelessSession ss;
	private ScrollableResults scroll;
	private ExtendedSearchIntegrator searchIntegrator;
	private EntityIndexBinding entityIndexBinding;
	private DocumentBuilderIndexedEntity docBuilder;

	@Inject
	public ItemReader(JobContext jobContext) {
		this.jobContext = jobContext;
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
		return checkpointId;
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
			logger.info( "Session closed" );
		}
		catch (Exception e) {
			logger.error( e );
		}
		try {
			em.close();
			logger.info( "EntityManager closed" );
		}
		catch (Exception e) {
			logger.error( e );
		}
		try {
			if ( emf != null ) {
				emf.close();
				logger.info( "EntityManagerFactory closed" );
			}
		}
		catch (Exception e) {
			logger.error( e );
		}
	}

	/**
	 * Initialize the environment. If checkpoint does not exist, then it should
	 * be the first open. If checkpoint exist, then it isn't the first open,
	 * save the input object "checkpoint" into "tempIDs".
	 *
	 * @param checkpoint The last checkpoint info saved in the batch runtime,
	 * previously given by checkpointInfo(). If this is the first start, then
	 * the checkpoint will be null, so does lastId.
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public void open(Serializable checkpoint) throws Exception {
		logger.infof( "open reader for entityName=%s", entityName );
		entityClazz = ( (JobContextData) jobContext.getTransientUserData() )
				.getIndexedType( entityName );

		try {
			String path = "java:comp/env/" + persistenceUnitName;
			em = (EntityManager) InitialContext.doLookup( path );
		}
		catch (NoInitialContextException e) {
			// TODO: is it a right way to do this ?
			logger.info( "This is a Java SE environment, "
					+ "using entity manager factory ..." );
			emf = Persistence.createEntityManagerFactory( persistenceUnitName );
			em = emf.createEntityManager();
		}

		session = em.unwrap( Session.class );
		ss = session.getSessionFactory().openStatelessSession();
		searchIntegrator = ContextHelper.getSearchintegrator( session );
		entityIndexBinding = searchIntegrator.getIndexBindings().get( entityClazz );
		docBuilder = entityIndexBinding.getDocumentBuilder();
		String idName = docBuilder.getIdentifierName();

		if ( checkpoint == null ) {
			scroll = ss.createCriteria( entityClazz )
					.addOrder( Order.asc( idName ) )
					.setReadOnly( true )
					.setCacheable( true )
					.setFetchSize( 1 )
					.setMaxResults( maxResults )
					.scroll( ScrollMode.FORWARD_ONLY );
		}
		else {
			checkpointId = checkpoint;
			scroll = ss.createCriteria( entityClazz )
					.add( Restrictions.gt( idName, checkpointId ) )
					.addOrder( Order.asc( idName ) )
					.setReadOnly( true )
					.setCacheable( true )
					.setFetchSize( 1 )
					.setMaxResults( maxResults )
					.scroll( ScrollMode.FORWARD_ONLY );
		}
	}

	/**
	 * Read item from database using JPA. Each read, there will be only one
	 * entity fetched.
	 *
	 * @throws Exception thrown for any errors.
	 */
	@Override
	public Object readItem() throws Exception {
		logger.info( "Reading item ..." );
		Object entity = null;
		AddLuceneWork addWork = null;
		if ( scroll.next() ) {
			entity = scroll.get( 0 );
			checkpointId = (Serializable) em.getEntityManagerFactory()
					.getPersistenceUnitUtil()
					.getIdentifier( entity );
			addWork = processItem( entity );
		}
		else {
			logger.info( "no more result. read ends." );
		}
		return addWork;
	}

	/**
	 * Process an input item into an output item. Here, the input item is an
	 * array of IDs and the output item is a list of Lucene works. During the
	 * process, entities are found by an injected entity manager, then they are
	 * used for building the correspondent Lucene works.
	 *
	 * @param item the input item, an array of IDs
	 * @return a list of Lucene works
	 * @throws Exception thrown for any errors.
	 */
	public AddLuceneWork processItem(Object item) throws Exception {
		AddLuceneWork addWork = buildAddLuceneWork( item, entityClazz );
		return addWork;
	}

	/**
	 * Build addLuceneWork using input entity. This method is inspired by the
	 * current mass indexer implementation.
	 *
	 * @param entity selected entity, obtained from JPA entity manager. It is
	 * used to build Lucene work.
	 * @param entityClazz the class type of selected entity
	 * @return an addLuceneWork
	 */
	private AddLuceneWork buildAddLuceneWork(Object entity,
			Class<?> entityClazz) {

		// TODO: tenant ID should not be null
		// Or may it be fine to be null? Gunnar's integration test in Hibernate
		// Search: MassIndexingTimeoutIT does not mention the tenant ID neither
		// (The tenant ID is not included mass indexer setup in the
		// ConcertManager)
		String tenantId = null;
		ConversionContext conversionContext = new ContextualExceptionBridgeHelper();
		final InstanceInitializer sessionInitializer = new HibernateSessionLoadingInitializer(
				(SessionImplementor) session );

		// Serializable id = session.getIdentifier(entity);
		Serializable id = (Serializable) em.getEntityManagerFactory()
				.getPersistenceUnitUtil()
				.getIdentifier( entity );
		TwoWayFieldBridge idBridge = docBuilder.getIdBridge();
		conversionContext.pushProperty( docBuilder.getIdKeywordName() );
		String idInString = null;
		try {
			idInString = conversionContext
					.setClass( entityClazz )
					.twoWayConversionContext( idBridge )
					.objectToString( id );
			logger.infof( "idInString=%s", idInString );
		}
		finally {
			conversionContext.popProperty();
		}
		AddLuceneWork addWork = docBuilder.createAddWork(
				tenantId,
				entityClazz,
				entity,
				id,
				idInString,
				sessionInitializer,
				conversionContext );
		return addWork;
	}
}
