package com.jzargo.productservice.mapper;

import com.jzargo.core.mapper.Mapper;
import com.jzargo.productservice.entity.Category;
import com.jzargo.productservice.model.CategoryDetails;
import org.springframework.stereotype.Component;

@Component
public class CategoryReadMapper implements Mapper<Category, CategoryDetails> {
    @Override
    public CategoryDetails map(Category from) {
        return CategoryDetails.builder()
                .attributes(from.getAttributes())
                .name(from.getName())
                .build();
    }
}
