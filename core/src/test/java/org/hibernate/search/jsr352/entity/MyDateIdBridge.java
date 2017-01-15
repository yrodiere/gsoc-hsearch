/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.entity;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;

/**
 * @author Mincong Huang
 */
public class MyDateIdBridge implements TwoWayFieldBridge {

	@Override
	public void set(String name, Object myDateIdObj, Document document, LuceneOptions luceneOptions) {
		MyDateId myDateId = (MyDateId) myDateIdObj;

		// cast int to string
		String year = String.format( "%04d", myDateId.getYear() );
		String month = String.format( "%02d", myDateId.getMonth() );
		String day = String.format( "%02d", myDateId.getDay() );

		// store each property in a unique field
		luceneOptions.addFieldToDocument( name + ".year", year, document );
		luceneOptions.addFieldToDocument( name + ".month", month, document );
		luceneOptions.addFieldToDocument( name + ".day", day, document );

		// store the unique string representation in the named field
		luceneOptions.addFieldToDocument( name, objectToString( myDateId ), document );
	}

	@Override
	public Object get(String name, Document document) {
		MyDateId myDateId = new MyDateId();
		IndexableField idxField;

		idxField = document.getField( name + ".year" );
		myDateId.setYear( Integer.valueOf( idxField.stringValue() ) );

		idxField = document.getField( name + ".month" );
		myDateId.setMonth( Integer.valueOf( idxField.stringValue() ) );

		idxField = document.getField( name + ".day" );
		myDateId.setDay( Integer.valueOf( idxField.stringValue() ) );

		return myDateId;
	}

	@Override
	public String objectToString(Object myDateIdObj) {
		MyDateId myDateId = (MyDateId) myDateIdObj;
		return new StringBuilder()
				.append( String.format( "%04d", myDateId.getYear() ) )
				.append( String.format( "%02d", myDateId.getMonth() ) )
				.append( String.format( "%02d", myDateId.getDay() ) )
				.toString();
	}
}
