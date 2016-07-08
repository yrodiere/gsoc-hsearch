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

	private final Set<Class<?>> entityTypesToIndex;

	public BatchContextData(Set<Class<?>> entityTypesToIndex) {
		this.entityTypesToIndex = entityTypesToIndex;
	}

	public Set<Class<?>> getEntityTypesToIndex() {
		return entityTypesToIndex;
	}

	public Class<?> getIndexedType(String entityType) throws ClassNotFoundException {
		for ( Class<?> clazz : entityTypesToIndex ) {
			if ( clazz.getName().equals( entityType ) ) {
				return clazz;
			}
		}

		String msg = String.format( "entityType %s not found.", entityType );
		throw new ClassNotFoundException( msg );
	}
}
