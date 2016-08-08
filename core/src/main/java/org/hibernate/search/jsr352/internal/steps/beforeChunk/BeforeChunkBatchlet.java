/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.beforeChunk;

import javax.batch.api.AbstractBatchlet;
import javax.batch.api.BatchProperty;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.Session;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.hcore.util.impl.ContextHelper;
import org.hibernate.search.jsr352.internal.JobContextData;
import org.jboss.logging.Logger;

/**
 * Enhancements before the chunk step "produceLuceneDoc" (lucene document
 * production)
 *
 * @author Mincong Huang
 */
@Named
public class BeforeChunkBatchlet extends AbstractBatchlet {

	private static final Logger logger = Logger.getLogger( BeforeChunkBatchlet.class );
	private final JobContext jobContext;

	@PersistenceUnit(unitName = "h2")
	private EntityManagerFactory emf;
	private EntityManager em;

	@Inject
	@BatchProperty
	private boolean purgeAtStart;

	@Inject
	@BatchProperty
	private boolean optimizeAfterPurge;

	@Inject
	public BeforeChunkBatchlet(JobContext jobContext) {
		this.jobContext = jobContext;
	}

	@Override
	public String process() throws Exception {

		if ( this.purgeAtStart ) {

			em = emf.createEntityManager();
			Session session = em.unwrap( Session.class );
			final BatchBackend backend = ContextHelper
					.getSearchintegrator( session )
					.makeBatchBackend( null );

			JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
			jobData.getEntityClazzSet()
					.forEach( clz -> backend.doWorkInSync( new PurgeAllLuceneWork( null, clz ) ) );

			if ( this.optimizeAfterPurge ) {
				logger.info( "optimizing all entities ..." );
				backend.optimize( jobData.getEntityClazzSet() );
			}
		}
		return null;
	}

	@Override
	public void stop() throws Exception {
		try {
			em.close();
		}
		catch ( Exception e ) {
			logger.error( e );
		}
	}
}
