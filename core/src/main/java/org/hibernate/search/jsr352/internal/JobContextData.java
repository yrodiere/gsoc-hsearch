/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.criterion.Criterion;
import org.hibernate.search.jsr352.internal.steps.lucene.PartitionProgress;
import org.hibernate.search.jsr352.internal.steps.lucene.StepProgress;
import org.hibernate.search.jsr352.internal.util.PartitionUnit;

/**
 * Container for data shared across the entire batch job.
 *
 * @author Gunnar Morling
 * @author Mincong Huang
 */
public class JobContextData implements Serializable {

	private static final long serialVersionUID = 4465274690302894983L;

	/**
	 * The map of key value pair (string, class-type), designed for storage of
	 * name and class type of all root entities. In JSR 352 standard, only
	 * string values can be propagated using job properties, but class types are
	 * frequently used too. So this map facilites this kind of lookup.
	 */
	private Map<String, Class<?>> entityClazzMap;

	/**
	 * Indexing progress for the step "produceLuceneDoc".
	 */
	private StepProgress stepProgress;

	/**
	 * The total number of entities to index over all the entity types.
	 */
	private long totalEntityToIndex;

	/**
	 * The list of units containing properties for each partition.
	 */
	private List<PartitionUnit> partitionUnits;

	private Set<Criterion> criterions;

	public JobContextData() {
		stepProgress = new StepProgress();
	}

	public void setEntityClazzSet(Set<Class<?>> entityClazzes) {
		entityClazzMap = new HashMap<>();
		entityClazzes.forEach( clz -> entityClazzMap.put( clz.toString(), clz ) );
	}

	public Set<String> getEntityNameSet() {
		return entityClazzMap.keySet();
	}

	public Set<Class<?>> getEntityClazzSet() {
		return new HashSet<Class<?>>( entityClazzMap.values() );
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

	public Set<Criterion> getCriterions() {
		return criterions;
	}

	public void setTotalEntityToIndex(long totalEntityToIndex) {
		this.totalEntityToIndex = totalEntityToIndex;
	}

	/**
	 * Increment to total entity number to index
	 *
	 * @param increment the entity number to index for one entity type
	 */
	public void incrementTotalEntity(long increment) {
		totalEntityToIndex += increment;
	}

	public void setPartitionUnits(List<PartitionUnit> partitionUnits) {
		this.partitionUnits = partitionUnits;
	}

	public PartitionUnit getPartitionUnit(int partitionID) {
		return partitionUnits.get( partitionID );
	}

	public long getRowsToIndex(String entityName) {
		return stepProgress.getRowsToIndex( entityName );
	}

	public void setRowsToIndex(String entityName, long rowsToIndex) {
		stepProgress.setRowsToIndex( entityName, rowsToIndex );
	}

	/**
	 * Update the step-level indexing progress using the partition-level
	 * indexing progress. (step-level is higher, one step contains multiple
	 * partitions)
	 *
	 * @param pp partition-level indexing progress
	 */
	public void updateStepProgress(PartitionProgress pp) {
		stepProgress.updateProgress( pp );
	}

	/**
	 * Get progresses of each entity at step-level.
	 *
	 * @return an iterable results in string format.
	 */
	public Iterable<String> getStepProgresses() {
		return stepProgress.getProgresses();
	}

	public void setCriterions(Set<Criterion> criterions) {
		this.criterions = criterions;
	}

	@Override
	public String toString() {
		return "JobContextData [entityClazzMap=" + entityClazzMap + ", stepProgress="
				+ stepProgress + ", totalEntityToIndex=" + totalEntityToIndex
				+ ", partitionUnits=" + partitionUnits + "]";
	}
}
