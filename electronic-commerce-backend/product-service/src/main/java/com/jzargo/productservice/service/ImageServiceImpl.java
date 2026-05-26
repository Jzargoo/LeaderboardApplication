package com.jzargo.productservice.service;


import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ImageServiceImpl implements ImageService {


    private final ImageDriver imageDriver;

    public ImageServiceImpl(ImageDriver imageDriver) {
        this.imageDriver = imageDriver;
    }

    @Override
    public void addImage(byte[] image, Long productId) throws IOException {
        String imageName = imageDriver.save_file(image);


    }
}
