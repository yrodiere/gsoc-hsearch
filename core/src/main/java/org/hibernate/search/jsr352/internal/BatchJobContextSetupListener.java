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

import org.hibernate.search.jpa.Search;

@Named
public class BatchJobContextSetupListener extends AbstractJobListener {

	private final JobContext jobContext;
	private final IndexingContext indexingContext;

	@Inject
	@BatchProperty
	private String rootEntities;

	@Inject
	public BatchJobContextSetupListener(JobContext jobContext,
			IndexingContext indexingContext) {
		this.jobContext = jobContext;
		this.indexingContext = indexingContext;
	}

	@Override
	public void beforeJob() throws Exception {
		Set<Class<?>> entitiesToIndex = new HashSet<>();

		String[] entityNamesToIndex = rootEntities.split( "," );
		Set<Class<?>> indexedTypes = Search
				.getFullTextEntityManager( indexingContext.getEntityManager() )
				.getSearchFactory()
				.getIndexedTypes();

		for ( String entityName : entityNamesToIndex ) {
			for ( Class<?> indexedType : indexedTypes ) {
				if ( indexedType.getName().equals( entityName.trim() ) ) {
					entitiesToIndex.add( indexedType );
					continue;
				}
			}
		}

		jobContext.setTransientUserData( new BatchContextData( entitiesToIndex ) );
	}
}
