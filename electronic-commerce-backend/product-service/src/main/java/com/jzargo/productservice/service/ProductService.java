package com.jzargo.productservice.service;

import com.jzargo.productservice.model.*;


public interface ProductService {
    ProductDetails getProductById(Long id);
    String createProduct(CreateAndUpdateProductDetails createProductDetails);
    ProductDetails updateProduct(CreateAndUpdateProductDetails updateProductDetails);
    String deleteProduct(Long productId);
}
