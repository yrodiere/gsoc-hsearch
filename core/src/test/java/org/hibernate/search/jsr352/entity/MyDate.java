/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.entity;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;

/**
 * @author Mincong Huang
 */
@Entity
@Indexed
public class MyDate {

	@EmbeddedId
	@DocumentId
	@FieldBridge(impl = MyDateIdBridge.class)
	private MyDateId myDateId;

	@Field
	private String weekday;

	public MyDate() {
	}

	public MyDate(int year, int month, int day) {
		this.myDateId = new MyDateId( year, month, day );
		this.weekday = MyDate.getWeekday( this.myDateId.toDate() );
	}

	public MyDateId getMyDateId() {
		return myDateId;
	}

	public void setMyDateId(MyDateId myDateId) {
		this.myDateId = myDateId;
	}

	public String getWeekday() {
		return weekday;
	}

	public static String getWeekday(Date date) {
		return new SimpleDateFormat( "EE" ).format( date ).toString();
	}

	public void setWeekday(String weekday) {
		this.weekday = weekday;
	}

	@Override
	public String toString() {
		return new StringBuilder()//
				.append( "MyDate [myDateId=" ).append( myDateId ) //
				.append( ", weekday=" ).append( weekday ) //
				.append( "]" ).toString();
	}
}
