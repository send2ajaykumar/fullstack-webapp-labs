package com.ajay.imageresolution.service;

import com.ajay.imageresolution.model.Image;
import com.ajay.imageresolution.model.Resolution;

public interface ImageService {

	Image getImage(int id);
	
	Image addImage(Image image);

	Resolution getImageResolution(int id);
}
