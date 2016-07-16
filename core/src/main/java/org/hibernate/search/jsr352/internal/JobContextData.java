/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Container for data shared across the entire batch job.
 *
 * @author Gunnar Morling
 * @author Mincong Huang
 */
public class JobContextData {

	private Map<String, Class<?>> entityClazzMap;
	private long totalEntityToIndex;

	public JobContextData(Set<Class<?>> entityClazzes) {
		entityClazzMap = new HashMap<>();
		entityClazzes.forEach( clz -> entityClazzMap.put( clz.toString(), clz ) );
	}

	public Set<String> getEntityNames() {
		return entityClazzMap.keySet();
	}

	public String[] getEntityNameArray() {
		Set<String> keySet = entityClazzMap.keySet();
		return keySet.toArray( new String[keySet.size()] );
	}

	public Class<?> getIndexedType(String entityName) throws ClassNotFoundException {
		Class<?> clazz = entityClazzMap.get( entityName );
		if ( clazz == null ) {
			String msg = String.format( "entityName %s not found.", entityName );
			throw new ClassNotFoundException( msg );
		}
		return clazz;
	}

	public long getTotalEntityToIndex() {
		return totalEntityToIndex;
	}

	public void setTotalEntityToIndex(long totalEntityToIndex) {
		this.totalEntityToIndex = totalEntityToIndex;
	}
}