package com.jzargo.productservice.service;

import com.jzargo.productservice.mapper.CategoryCreateAndUpdateMapper;
import com.jzargo.productservice.mapper.ReadProductDetailsMapper;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import com.jzargo.productservice.model.ProductDetails;
import com.jzargo.productservice.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService{

    private final ProductRepository productRepository;
    private final ReadProductDetailsMapper readProductDetailsMapper;
    private final CategoryCreateAndUpdateMapper categoryCreateAndUpdateMapper;

    public ProductServiceImpl(ProductRepository productRepository, ReadProductDetailsMapper readProductDetailsMapper, CategoryCreateAndUpdateMapper categoryCreateAndUpdateMapper) {
        this.productRepository = productRepository;
        this.readProductDetailsMapper = readProductDetailsMapper;
        this.categoryCreateAndUpdateMapper = categoryCreateAndUpdateMapper;
    }

    @Override
    public ProductDetails getProductById(Long id) {
        return productRepository
                .findById(id)
                .map(readProductDetailsMapper::map)
                .orElseThrow();
    }

    @Transactional
    @Override
    public String createProduct(CreateAndUpdateProductDetails createProductDetails) {
        return "Creation of the process started successfully";
    }

    @Override
    @Transactional
    public ProductDetails updateProduct(CreateAndUpdateProductDetails updateProductDetails) {
        //  productRepository.findById(updateProductDetails)
        return null;
    }

    @Transactional
    @Override
    public String deleteProduct(Long productId) {
        return "Deletion of the process started successfully";
    }
}
