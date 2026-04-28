package com.ajay.imageresolution.service;

import org.springframework.stereotype.Service;

import com.ajay.imageresolution.model.Image;
import com.ajay.imageresolution.model.Resolution;

@Service
public class ImageServiceImpl implements ImageService{

	@Override
	public Image getImage(int id) {
		// TODO Auto-generated method stub
		return new Image(1, "name1", "desc1");
	}

	@Override
	public Image addImage(Image image) {
		//Going to add this image to database;
		System.out.println("The image to be saved is :" + image.toString());		
		return new Image(2, "name2", "desc2");
	}

	@Override
	public Resolution getImageResolution(int id) {
		Resolution r = new Resolution(1920, 1020, "jpg");
		System.out.println("The image resolution is :" + r.toString());
		return r;
	}
}
