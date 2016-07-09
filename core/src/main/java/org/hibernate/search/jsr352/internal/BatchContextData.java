/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal;

import java.util.Map;
import java.util.Set;

import org.hibernate.search.store.IndexShardingStrategy;

/**
 * Container for data shared across the entire batch.
 *
 * @author Gunnar Morling
 * @author Mincong HUANG
 */
public class BatchContextData {

	private Set<Class<?>> entityClazzesToIndex;
	private Map<String, IndexShardingStrategy> shardingStrategyMap;

	public BatchContextData( Set<Class<?>> entityClazzes,
			Map<String, IndexShardingStrategy> shardingStrategyMap) {
		this.entityClazzesToIndex = entityClazzes;
		this.shardingStrategyMap = shardingStrategyMap;
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
	
	public IndexShardingStrategy getShardingStrategy( String entityName )
			throws ClassNotFoundException {
		IndexShardingStrategy strategy = shardingStrategyMap.get( entityName );
		if ( strategy == null ) {
			String msg = String.format( "IndexShardingStrategy not found "
					+ "for class %s.", entityName );
			throw new ClassNotFoundException( msg );
		}
		return strategy;
	}
}
