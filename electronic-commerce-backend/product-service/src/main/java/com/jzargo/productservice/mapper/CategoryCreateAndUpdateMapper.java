package com.jzargo.productservice.mapper;

import com.jzargo.core.mapper.Mapper;
import com.jzargo.productservice.entity.Category;
import com.jzargo.productservice.model.CreateAndUpdateCategoryDetails;
import org.springframework.stereotype.Component;

@Component
public class CategoryCreateAndUpdateMapper implements Mapper<CreateAndUpdateCategoryDetails, Category> {

    @Override
    public Category map(CreateAndUpdateCategoryDetails from) {
        return Category.builder()
                .name(from.getName())
                .attributes(from.getAttributes())
                .build();
    }
}
