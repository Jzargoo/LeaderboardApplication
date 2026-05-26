package com.jzargo.productservice.service;

import java.io.IOException;

public interface ImageDriver {
    String save_file(byte[] image) throws IOException;
}
