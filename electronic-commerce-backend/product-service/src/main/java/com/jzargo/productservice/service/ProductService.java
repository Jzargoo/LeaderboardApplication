package com.jzargo.productservice.service;

import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.model.*;


public interface ProductService {
    ProductDetails getProductById(Long id) throws ProductNotFoundException;
    String createProduct(CreateAndUpdateProductDetails createProductDetails);
    ProductDetails updateProduct(CreateAndUpdateProductDetails updateProductDetails) throws ProductNotFoundException;
    String deleteProduct(Long productId);
}
