/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.util;

/**
 * Information about a target partition which can not be stored in the partition properties as String values. In
 * particular, the boundary properties help us to identify the lower boundary and upper boundary of a given partition,
 * with which the two ends of the scrollable results can be defined and be applied to
 * {@link org.hibernate.search.jsr352.internal.steps.lucene.ItemReader#open}.
 *
 * @author Mincong Huang
 */
public class PartitionBound {

	private Class<?> entityClazz;
	private Object lowerBound;
	private Object upperBound;

	public PartitionBound() {
	}

	public PartitionBound(Class<?> entityClazz) {
		this.entityClazz = entityClazz;
	}

	public PartitionBound(Class<?> entityClazz, Object lowerBound, Object upperBound) {
		this.entityClazz = entityClazz;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public Class<?> getEntityClazz() {
		return entityClazz;
	}

	public String getEntityName() {
		return entityClazz.getName();
	}

	public Object getLowerBound() {
		return lowerBound;
	}

	public Object getUpperBound() {
		return upperBound;
	}

	public boolean isFirstPartition() {
		return lowerBound == null && upperBound != null;
	}

	public boolean isLastPartition() {
		return lowerBound != null && upperBound == null;
	}

	public boolean isUniquePartition() {
		return lowerBound == null && upperBound == null;
	}

	public void setEntityClazz(Class<?> entityClazz) {
		this.entityClazz = entityClazz;
	}

	@Override
	public String toString() {
		return "PartitionBound [entityClazz=" + entityClazz + ", lowerBound=" + lowerBound + ", upperBound=" + upperBound + "]";
	}
}
