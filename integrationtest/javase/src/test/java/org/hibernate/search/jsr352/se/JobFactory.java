/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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
