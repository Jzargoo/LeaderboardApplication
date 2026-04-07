package com.jzargo.productservice.api;

import com.jzargo.productservice.model.*;
import com.jzargo.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    ResponseEntity<ProductDetails>  getProductById(@PathVariable Long id){
        try {
            return ResponseEntity.ok(productService.getProductById(id));
        } catch (Exception e) {
            return ResponseEntity.noContent().build();
        }
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

    @DeleteMapping("/{id}")
    ResponseEntity<String> deleteProduct(@PathVariable String id){
        return ResponseEntity.ok("");
    }
}
