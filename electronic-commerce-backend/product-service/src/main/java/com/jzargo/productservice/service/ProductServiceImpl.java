package com.jzargo.productservice.service;

import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.mapper.ProductCreateAndUpdateMapper;
import com.jzargo.productservice.mapper.ReadProductDetailsMapper;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import com.jzargo.productservice.model.ProductDetails;
import com.jzargo.productservice.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true) // if not provided, data must be in immutable state
public class ProductServiceImpl implements ProductService{

    private final ProductRepository productRepository;
    private final ReadProductDetailsMapper readProductDetailsMapper;
    private final ProductCreateAndUpdateMapper productCreateAndUpdateMapper;

    public ProductServiceImpl(ProductRepository productRepository, ReadProductDetailsMapper readProductDetailsMapper, ProductCreateAndUpdateMapper productCreateAndUpdateMapper) {
        this.productRepository = productRepository;
        this.readProductDetailsMapper = readProductDetailsMapper;
        this.productCreateAndUpdateMapper = productCreateAndUpdateMapper;
    }

    @Override
    public ProductDetails getProductById(Long id) throws ProductNotFoundException{
        return productRepository
                .findById(id)
                .map(readProductDetailsMapper::map)
                .orElseThrow(ProductNotFoundException::new);
    }

    @Transactional
    @Override
    public String createProduct(CreateAndUpdateProductDetails createProductDetails) {
        return "Creation of the product started successfully";
    }

    @Override
    @Transactional
    public ProductDetails updateProduct(CreateAndUpdateProductDetails updateProductDetails) throws ProductNotFoundException{

        // If null -> the data did not change

        // TODO: fill nulls with previous info fields that did not change
        Product newProduct = productRepository
                .findById(updateProductDetails.getId())
                .map((product) -> productCreateAndUpdateMapper.map(updateProductDetails, product))
                .orElseThrow(ProductNotFoundException::new);

        // TODO: implement update via table for Debezium

        return readProductDetailsMapper.map(newProduct);
    }

    @Transactional
    @Override
    public String deleteProduct(Long productId) {
        return "Deletion of the process started successfully";
    }
}
