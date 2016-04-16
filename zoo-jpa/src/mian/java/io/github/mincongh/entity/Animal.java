package io.github.mincongh.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;


/**
 * The persistent class for the animal database table.
 * 
 * @author Mincong HUANG
 */
@Entity
@Indexed(index="indexes/animals")
@Table(name="animal")
@NamedQuery(name="Animal.findAll", query="SELECT a FROM Animal a")
public class Animal implements Serializable {
    
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue
	private int id;
	
	@Field
	private String gender;

    @Column(name="image_path")
	private String imagePath;
	
	@Field
	private String name;
	
	private String talk;
	
	@Field
	private String type;

	public Animal() {
	}
	
	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getGender() {
		return this.gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getImagePath() {
		return this.imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public String getTalk() {
		return this.talk;
	}

	public void setTalk(String talk) {
		this.talk = talk;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

    @Override
    public String toString() {
        return "Animal [id=" + id + ", gender=" + gender + ", imagePath="
                + imagePath + ", name=" + name + ", talk=" + talk + ", type=" 
                + type + "]";
    }	
}