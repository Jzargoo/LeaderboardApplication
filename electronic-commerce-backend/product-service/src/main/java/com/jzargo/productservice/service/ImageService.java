package com.jzargo.productservice.service;

import com.jzargo.productservice.exception.ProductNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ImageService {

    void addImages(List<MultipartFile> images, Long productId, Long shopId)
            throws IOException, ProductNotFoundException;

    void addAvatar(byte[] image, Long productId, Long shopId)
            throws IOException, ProductNotFoundException;

    List<byte[]> getAllImages(Long productId)
            throws IOException, ProductNotFoundException;

    byte[] getAvatar(Long productId)
                throws IOException, ProductNotFoundException;

}
