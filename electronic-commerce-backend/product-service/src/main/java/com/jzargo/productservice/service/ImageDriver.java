package com.jzargo.productservice.service;

import java.io.IOException;

public interface ImageDriver {
    String saveFile(byte[] image) throws IOException;
}
