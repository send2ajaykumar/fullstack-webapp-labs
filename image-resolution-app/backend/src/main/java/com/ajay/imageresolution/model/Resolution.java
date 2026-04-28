package com.ajay.imageresolution.model;

public class Resolution {
	private int width;
	private int height;
	private String format;
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	public String getFormat() {
		return format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	public Resolution(int width, int height, String format) {
		super();
		this.width = width;
		this.height = height;
		this.format = format;
	}
	public Resolution() {
		super();
		// TODO Auto-generated constructor stub
	}
	@Override
	public String toString() {
		return "Resolution [width=" + width + ", height=" + height + ", format=" + format + "]";
	}
	
	
}
