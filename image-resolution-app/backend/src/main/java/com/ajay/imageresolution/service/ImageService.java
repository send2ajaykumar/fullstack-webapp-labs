package com.ajay.imageresolution.service;

import com.ajay.imageresolution.model.Image;

public interface ImageService {

	Image getImage(int id);
	
	Image addImage(Image image);
}
