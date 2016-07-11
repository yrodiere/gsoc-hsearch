/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal;

import java.util.HashSet;
import java.util.Set;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.AbstractJobListener;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.InitialContext;
import javax.naming.NoInitialContextException;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.hibernate.search.jpa.Search;
import org.jboss.logging.Logger;

@Named
public class JobContextSetupListener extends AbstractJobListener {

	private static final Logger logger = Logger.getLogger( JobContextSetupListener.class );
	private final JobContext jobContext;

	@Inject
	@BatchProperty
	private String rootEntities;
	
	@Inject
	@BatchProperty
	private String persistenceUnitName;

	@Inject
	public JobContextSetupListener(JobContext jobContext) {
		this.jobContext = jobContext;
	}

	@Override
	public void beforeJob() throws Exception {
		EntityManager em;
		try {
			String path = "java:comp/env/" + persistenceUnitName;
			em = (EntityManager) InitialContext.doLookup( path );
		} catch (NoInitialContextException e) {
			// TODO: is it a right way to do this ?
			logger.info("This is a Java SE environment, "
					+ "using entity manager factory ..." );
			em = Persistence.createEntityManagerFactory( persistenceUnitName )
					.createEntityManager();
		}

		String[] entityNamesToIndex = rootEntities.split( "," );
		Set<Class<?>> entityClazzesToIndex = new HashSet<>();
		Set<Class<?>> indexedTypes = Search
				.getFullTextEntityManager( em )
				.getSearchFactory()
				.getIndexedTypes();

		for ( String entityName : entityNamesToIndex ) {
			for ( Class<?> indexedType : indexedTypes ) {
				if ( indexedType.getName().equals( entityName.trim() ) ) {
					entityClazzesToIndex.add( indexedType );
					continue;
				}
			}
		}

		jobContext.setTransientUserData( new JobContextData( entityClazzesToIndex ) );
	}
}
