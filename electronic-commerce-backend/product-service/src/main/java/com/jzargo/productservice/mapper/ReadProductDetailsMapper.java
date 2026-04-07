package com.jzargo.productservice.mapper;

import com.jzargo.core.mapper.Mapper;
import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.model.ProductDetails;
import org.springframework.stereotype.Component;

@Component
public class ReadProductDetailsMapper implements Mapper<Product, ProductDetails> {

    @Override
    public ProductDetails map(Product from) {
        return ProductDetails.builder()
                .category(from.getCategory().getName())
                .name(from.getName())
                .description(from.getDescription())
                .characteristics(from.getCharacteristics())
                .build();
    }
}
