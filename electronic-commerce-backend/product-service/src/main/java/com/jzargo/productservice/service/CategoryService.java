package com.jzargo.productservice.service;

import com.jzargo.productservice.exception.MalformedDataError;
import com.jzargo.productservice.model.CategoryDetails;
import com.jzargo.productservice.model.CreateAndUpdateCategoryDetails;

import java.util.List;

public interface CategoryService {
    CategoryDetails createCategory(CreateAndUpdateCategoryDetails createCategoryDetails) throws MalformedDataError;
    List<String> getCategories();
}
