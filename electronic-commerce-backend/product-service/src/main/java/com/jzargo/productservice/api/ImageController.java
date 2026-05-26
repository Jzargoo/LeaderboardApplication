package com.jzargo.productservice.api;

import com.jzargo.productservice.service.ImageService
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {
    private final ImageService imageService;

    @PostMapping("/{productId}")
    public ResponseEntity<String> addImage(
            @RequestBody MultipartFile multipartFile,
            @PathVariable Long productId) {
        try {
            imageService.addImage(multipartFile.getBytes(), productId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(
                "new image of the product was added successfully"
        );
    }
}
