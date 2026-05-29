package com.jzargo.productservice.unit;


import com.jzargo.productservice.repository.ProductRepository;
import com.jzargo.productservice.service.ImageDriverNative;
import com.jzargo.productservice.service.ImageServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ImageServiceUnitTest {

    @Mock
    private ImageDriverNative imageDriverNative;

    @InjectMocks
    private ImageServiceImpl imageService;

    @Mock
    private ProductRepository productRepository;

    @Test
    public
}
