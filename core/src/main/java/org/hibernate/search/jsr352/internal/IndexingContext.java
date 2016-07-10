/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;

/**
 * Specific indexing context for mass indexer. Several attributes are used :
 * <p>
 * <ul>
 * <li>entityCount: the total number of entities to be indexed in the job. The
 * number is summarized by partitioned step "loadId". Each IdProducerBatchlet
 * (partiton) produces the number of entities linked to its own target entity,
 * then call the method #addEntityCount(long) to summarize it with other
 * partition(s).</li>
 * </ul>
 * 
 * @author Mincong HUANG
 */
@Named
@Singleton
public class IndexingContext {

	private EntityManager entityManager;

	public IndexingContext() {
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}
}
