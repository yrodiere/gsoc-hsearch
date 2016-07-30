/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.util;

import java.util.Comparator;

/**
 * @author Mincong Huang
 */
public class StringComparator implements Comparator<PartitionUnit> {

	@Override
	public int compare(PartitionUnit x, PartitionUnit y) {
		String entityNameX = x.getEntityName();
		String entityNameY = y.getEntityName();
		return entityNameX.compareTo( entityNameY );
	}
}