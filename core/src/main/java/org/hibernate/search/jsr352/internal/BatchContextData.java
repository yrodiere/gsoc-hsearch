/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal;

import java.util.Set;

/**
 * Container for data shared across the entire batch.
 *
 * @author Gunnar Morling
 */
public class BatchContextData {

	private Set<Class<?>> entityClazzesToIndex;

	public BatchContextData(Set<Class<?>> entityClazzes) {
		this.entityClazzesToIndex = entityClazzes;
	}

	public Set<Class<?>> getEntityTypesToIndex() {
		return entityClazzesToIndex;
	}

	public Class<?> getIndexedType(String entityName) throws ClassNotFoundException {
		for ( Class<?> clazz : entityClazzesToIndex ) {
			if ( clazz.getName().equals( entityName ) ) {
				return clazz;
			}
		}
		String msg = String.format( "entityName %s not found.", entityName );
		throw new ClassNotFoundException( msg );
	}
}
