package com.jzargo.productservice.api;

import com.jzargo.productservice.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("api/products")
public class ProductController {
    @GetMapping
    ResponseEntity<Page<ProductInformation>> getAllByFilter(
            @RequestParam ProductFilter productFilter,
            @RequestHeader("${application.headers.part-filtered}") List<Long> partiallyFiltered
    ) {

        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}")
    ResponseEntity<ProductDetails>  getProductById(@PathVariable Long id){
        return null;
    }

    @PostMapping
    ResponseEntity<String> createProduct (@RequestBody CreateAndUpdateProductDetails createProductDetails) {
        return ResponseEntity.ok("Product is initialized");
    }

    @PutMapping("/{id}")
    ResponseEntity<ProductDetails> updateProduct(
            @PathVariable Long id,
            @RequestBody CreateAndUpdateProductDetails createAndUpdateProductDetails
            ) {
        return ResponseEntity.ok(new ProductDetails());
    }

    @GetMapping("/categories")
    ResponseEntity<List<String>> getCategories (){
        List<String> categories = new ArrayList<>();
        categories.add("Electronics");
        return ResponseEntity.ok(
                categories
        );
    }

    @DeleteMapping("/{id}")
    ResponseEntity<String> deleteProduct(@PathVariable String id){
        return ResponseEntity.ok("");
    }
}
