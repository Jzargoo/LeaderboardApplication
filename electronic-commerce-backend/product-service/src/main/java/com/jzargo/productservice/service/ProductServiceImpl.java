package com.jzargo.productservice.service;

import com.jzargo.productservice.mapper.ReadProductDetailsMapper;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import com.jzargo.productservice.model.ProductDetails;
import com.jzargo.productservice.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService{

    private final ProductRepository productRepository;
    private final ReadProductDetailsMapper readProductDetailsMapper;

    public ProductServiceImpl(ProductRepository productRepository, ReadProductDetailsMapper readProductDetailsMapper) {
        this.productRepository = productRepository;
        this.readProductDetailsMapper = readProductDetailsMapper;
    }

    @Override
    public ProductDetails getProductById(Long id) {
        return productRepository
                .findById(id)
                .map(readProductDetailsMapper::map)
                .orElseThrow();
    }

    @Override
    public String createProduct(CreateAndUpdateProductDetails createProductDetails) {
        return "";
    }

    @Override
    public ProductDetails updateProduct(CreateAndUpdateProductDetails updateProductDetails) {
        return null;
    }

    @Override
    public String deleteProduct(Long productId) {
        return "";
    }
}
