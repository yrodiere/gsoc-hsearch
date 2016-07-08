package org.hibernate.search.jsr352.se;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;

/**
 *
 * @author Mincong HUANG
 */
public class JobFactory {

	private static JobOperator jobOperator = BatchRuntime.getJobOperator();

	public static JobOperator getJobOperator() {
		return jobOperator;
	}
}
