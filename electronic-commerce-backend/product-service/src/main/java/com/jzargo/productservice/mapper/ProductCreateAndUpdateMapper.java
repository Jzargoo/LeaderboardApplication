package com.jzargo.productservice.mapper;

import com.jzargo.core.mapper.Mapper;
import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.model.CreateAndUpdateProductDetails;
import org.springframework.stereotype.Component;

@Component
public class ProductCreateAndUpdateMapper implements Mapper<CreateAndUpdateProductDetails, Product> {

    @Override
    public Product map(CreateAndUpdateProductDetails from) {
        return Product.builder()
                .name(from.getName())
                .description(from.getDescription())
                .characteristics(from.getCharacteristics())
                .stockPrice(from.getPrice())
                .build();
    }

    @Override
    public Product map(CreateAndUpdateProductDetails from, Product old) {
        return Mapper.super.map(from, old);
    }

}