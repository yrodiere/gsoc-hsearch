/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.entity;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import javax.persistence.Embeddable;

/**
 * Primary key for {@link MyDate}.
 *
 * @author Mincong Huang
 */
@Embeddable
public class MyDateId implements Serializable {

	private static final long serialVersionUID = -3941766084997859100L;

	private int year;
	private int month;
	private int day;

	public MyDateId() {

	}

	public MyDateId(int year, int month, int day) {
		this.year = year;
		this.month = month;
		this.day = day;
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public int getDay() {
		return day;
	}

	public void setDay(int day) {
		this.day = day;
	}

	public Date toDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.set( Calendar.YEAR, year );
		calendar.set( Calendar.MONTH, month - 1 ); // month is base-0
		calendar.set( Calendar.DAY_OF_MONTH, day );
		return calendar.getTime();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + day;
		result = prime * result + month;
		result = prime * result + year;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		MyDateId other = (MyDateId) obj;
		if ( day != other.day )
			return false;
		if ( month != other.month )
			return false;
		if ( year != other.year )
			return false;
		return true;
	}

	@Override
	public String toString() {
		return new StringBuilder().append( "MyDateId [year=" ).append( year ) //
				.append( ", month=" ).append( month ) //
				.append( ", day=" ).append( day ) //
				.append( "]" ).toString();
	}
}
