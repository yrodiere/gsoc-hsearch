/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal;

import org.hibernate.search.store.IndexShardingStrategy;

/**
 * Container for data specific to the entity indexing batch step.
 *
 * @author Gunnar Morling
 */
public class EntityIndexingStepData {

	private final Class<?> entityClass;
	private final IndexShardingStrategy shardingStrategy;

	private int processedWorkCount = 0;

	public EntityIndexingStepData(Class<?> entityClass, IndexShardingStrategy shardingStrategy) {
		this.entityClass = entityClass;
		this.shardingStrategy = shardingStrategy;
	}

	public Class<?> getEntityClass() {
		return entityClass;
	}

	public IndexShardingStrategy getShardingStrategy() {
		return shardingStrategy;
	}

	public void incrementProcessedWorkCount(int increment) {
		processedWorkCount += increment;
	}

	public int getProcessedWorkCount() {
		return processedWorkCount;
	}
}
