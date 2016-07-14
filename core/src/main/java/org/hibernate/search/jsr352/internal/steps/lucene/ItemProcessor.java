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
import javax.naming.NamingException;
import javax.persistence.EntityManager;

import org.hibernate.Session;
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
 * @author Mincong HUANG
 */
@Named
public class ItemProcessor implements javax.batch.api.chunk.ItemProcessor {

	private static final Logger logger = Logger.getLogger( ItemProcessor.class );
	private boolean isSetup = false;

	@Inject
	@BatchProperty
	private String entityName;

	@Inject
	@BatchProperty
	private String persistenceUnitName;

	private Class<?> entityClazz;
	private final JobContext jobContext;

	// read entities and produce Lucene work
	private EntityManager em;
	private Session session;
	private ExtendedSearchIntegrator searchIntegrator;
	private EntityIndexBinding entityIndexBinding;
	private DocumentBuilderIndexedEntity docBuilder;

	@Inject
	public ItemProcessor(JobContext jobContext) {
		this.jobContext = jobContext;
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
	public Object processItem(Object item) throws Exception {
		logger.info( "processing item ..." );
		if ( !isSetup ) {
			setup();
			isSetup = true;
		}
		AddLuceneWork addWork = buildAddLuceneWork( item, entityClazz );
		return addWork;
	}

	/**
	 * Set up environment for lucene work production.
	 *
	 * @throws ClassNotFoundException if the entityName does not match any
	 * indexed class type in the job context data.
	 * @throws NamingException if JNDI lookup for entity manager failed
	 */
	private void setup() throws ClassNotFoundException, NamingException {

		entityClazz = ( (JobContextData) jobContext.getTransientUserData() )
				.getIndexedType( entityName );

		String path = "java:comp/env/" + persistenceUnitName;
		em = (EntityManager) InitialContext.doLookup( path );

		session = em.unwrap( Session.class );
		searchIntegrator = ContextHelper.getSearchintegrator( session );
		entityIndexBinding = searchIntegrator.getIndexBindings().get( entityClazz );
		docBuilder = entityIndexBinding.getDocumentBuilder();
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
	private AddLuceneWork buildAddLuceneWork(Object entity, Class<?> entityClazz) {

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
