package com.jzargo.productservice.service;

import com.jzargo.productservice.exception.ProductNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ImageService {

    void addImages(byte[][] images, Long productId, Long shopId)
            throws IOException, ProductNotFoundException;

    void addAvatar(byte[] image, Long productId, Long shopId)
            throws IOException, ProductNotFoundException;

    List<byte[]> getImages(Long productId)
            throws IOException, ProductNotFoundException;

    byte[] getAvatar(Long productId)
                throws IOException, ProductNotFoundException;

}
