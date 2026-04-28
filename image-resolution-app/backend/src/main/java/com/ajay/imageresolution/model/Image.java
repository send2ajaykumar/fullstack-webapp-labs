package com.ajay.imageresolution.model;

public class Image {
	private int id;
	private String name;
	private String size;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSize() {
		return size;
	}
	public void setSize(String size) {
		this.size = size;
	}
	
	@Override
	public String toString() {
		return "Image [id=" + id + ", name=" + name + ", size=" + size + "]";
	}
	
	public Image(int id, String name, String size) {
		super();
		this.id = id;
		this.name = name;
		this.size = size;
	}
	
	public Image() {
		super();
		// TODO Auto-generated constructor stub
	}
}
