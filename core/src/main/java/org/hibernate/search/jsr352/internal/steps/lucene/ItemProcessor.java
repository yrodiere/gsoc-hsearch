/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import javax.inject.Named;
import org.jboss.logging.Logger;

/**
 * @author Mincong HUANG
 */
@Named
public class ItemProcessor implements javax.batch.api.chunk.ItemProcessor {

	private static final Logger logger = Logger.getLogger( ItemProcessor.class );

	@Override
	public Object processItem(Object item) throws Exception {
		logger.info( "processing item ..." );
		return item;
	}
}
