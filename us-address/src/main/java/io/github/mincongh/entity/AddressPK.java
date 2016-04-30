package io.github.mincongh.entity;

import java.io.Serializable;
import javax.persistence.*;

/**
 * The primary key class for the Address database table.
 * 
 */
@Embeddable
public class AddressPK implements Serializable {
	//default serial version id, required for serializable classes.
	private static final long serialVersionUID = 1L;

	@Column(columnDefinition="char(10)")
	private String id;

	@Column(columnDefinition="char(3)")
	private String seq;

	public AddressPK() {
	}
	public String getId() {
		return this.id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getSeq() {
		return this.seq;
	}
	public void setSeq(String seq) {
		this.seq = seq;
	}

	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AddressPK)) {
			return false;
		}
		AddressPK castOther = (AddressPK)other;
		return 
			this.id.equals(castOther.id)
			&& this.seq.equals(castOther.seq);
	}

	public int hashCode() {
		final int prime = 31;
		int hash = 17;
		hash = hash * prime + this.id.hashCode();
		hash = hash * prime + this.seq.hashCode();
		
		return hash;
	}
}