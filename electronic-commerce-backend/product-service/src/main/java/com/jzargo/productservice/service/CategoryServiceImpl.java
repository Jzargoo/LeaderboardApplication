package com.jzargo.productservice.service;

import com.jzargo.productservice.entity.Category;
import com.jzargo.productservice.exception.MalformedDataError;
import com.jzargo.productservice.mapper.CategoryCreateAndUpdateMapper;
import com.jzargo.productservice.mapper.CategoryReadMapper;
import com.jzargo.productservice.model.CategoryDetails;
import com.jzargo.productservice.model.CreateAndUpdateCategoryDetails;
import com.jzargo.productservice.repository.CategoryRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Data
@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService{

    private final CategoryRepository categoryRepository;
    private final CategoryCreateAndUpdateMapper categoryCreateAndUpdateMapper;
    private final CategoryReadMapper categoryReadMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryCreateAndUpdateMapper categoryCreateAndUpdateMapper, CategoryReadMapper categoryReadMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryCreateAndUpdateMapper = categoryCreateAndUpdateMapper;
        this.categoryReadMapper = categoryReadMapper;
    }

    @Override
    public CategoryDetails createCategory(
            CreateAndUpdateCategoryDetails createCategoryDetails
    ) throws MalformedDataError {

        if (createCategoryDetails.getName() == null ||
                (createCategoryDetails.getAttributes().isEmpty() &&
                        createCategoryDetails.getParentId() == null)
        ) {
            log.error("");
            throw new MalformedDataError();
        }

        Category save = categoryRepository.save(
                categoryCreateAndUpdateMapper.map(
                        createCategoryDetails
                )
        );

        log.info("New category {save} was created");

        return categoryReadMapper.map(save);



    }
}
