/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

/**
 * Container for data specific to the entity indexing batch step.
 *
 * @author Gunnar Morling
 * @author Mincong HUANG
 */
public class LuceneData {

	private int processedWorkCount = 0;

	public LuceneData() {
	}

	public void incrementProcessedWorkCount(int increment) {
		processedWorkCount += increment;
	}

	public int getProcessedWorkCount() {
		return processedWorkCount;
	}
}
