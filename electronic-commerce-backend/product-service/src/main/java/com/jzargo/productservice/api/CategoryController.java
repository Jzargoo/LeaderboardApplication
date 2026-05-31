package com.jzargo.productservice.api;

import com.jzargo.productservice.exception.MalformedDataError;
import com.jzargo.productservice.model.CategoryDetails;
import com.jzargo.productservice.model.CreateAndUpdateCategoryDetails;
import com.jzargo.productservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products/category")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryDetails> createCategory(
            @RequestBody CreateAndUpdateCategoryDetails createCategoryDetails
    ) {
        log.debug("Creation a category started to execute");

        try {

            return ResponseEntity.ok(
                    categoryService.createCategory(createCategoryDetails)
            );

        } catch (MalformedDataError e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
