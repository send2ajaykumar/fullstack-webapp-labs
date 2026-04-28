package com.ajay.imageresolution.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ajay.imageresolution.model.Image;
import com.ajay.imageresolution.model.Resolution;
import com.ajay.imageresolution.service.ImageService;

@RestController
@RequestMapping("/image")
public class ImageResolutionController {
	
	@Autowired
	private ImageService imageService;
	
	@GetMapping("/hello")
    public String helloWorld() {         
        // Returning a simple "Hello World" response
        return "Hello Buddy11"; 
    }
	
	@GetMapping("/{id}")
    public Image getImage(@PathVariable int id) {
		return imageService.getImage(id);
    }
	
	@GetMapping("/{id}/metadata")
    public Resolution getImageMetadata(@PathVariable int id) {
		return imageService.getImageResolution(id);
    }
	
	@PostMapping()
    public Image postImage(@RequestBody Image img) {
		return imageService.addImage(img);
    } 		
}
