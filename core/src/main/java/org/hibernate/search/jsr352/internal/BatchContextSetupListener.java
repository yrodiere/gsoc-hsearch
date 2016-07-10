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
import javax.persistence.EntityManager;

import org.hibernate.search.jpa.Search;

@Named
public class BatchContextSetupListener extends AbstractJobListener {

	private final JobContext jobContext;
	private EntityManager em;

	@Inject
	@BatchProperty
	private String rootEntities;

	@Inject
	public BatchContextSetupListener(JobContext jobContext,
			IndexingContext indexingContext) {
		this.jobContext = jobContext;
		this.em = indexingContext.getEntityManager();
	}

	@Override
	public void beforeJob() throws Exception {
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
