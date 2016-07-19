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
import javax.persistence.EntityManager;

import org.hibernate.search.jpa.Search;
import org.jboss.logging.Logger;

/**
 * Listener before the start of the job. It aims to setup the job context data,
 * shared by all the steps.
 *
 * @author Mincong Huang
 */
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

		String path = "java:comp/env/" + persistenceUnitName;
		logger.infof( "JNDI lookup for persistenceUnitName=\"%s\"...", path );

		EntityManager em = (EntityManager) InitialContext.doLookup( path );
		String[] entityNamesToIndex = rootEntities.split( "," );
		Set<Class<?>> entityClazzesToIndex = new HashSet<>();
		Set<Class<?>> indexedTypes = Search
				.getFullTextEntityManager( em )
				.getSearchFactory()
				.getIndexedTypes();

		// check the root entities selected do exist
		// in full-text entity session
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
