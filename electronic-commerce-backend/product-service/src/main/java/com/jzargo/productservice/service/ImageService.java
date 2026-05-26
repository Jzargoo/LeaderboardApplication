package com.jzargo.productservice.service;

import java.io.IOException;

public interface ImageService {
    void addImage(byte[] image,Long productId) throws IOException;
}
