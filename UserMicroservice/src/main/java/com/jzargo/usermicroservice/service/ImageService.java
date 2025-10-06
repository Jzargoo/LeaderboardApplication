package com.jzargo.usermicroservice.service;

public interface ImageService {
    String saveImage(byte[] image);

    void deleteImage(String avatar);
}
