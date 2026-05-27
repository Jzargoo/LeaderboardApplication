package com.jzargo.productservice.api;

import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.service.ImageService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {
    private final ImageService imageService;

    @PutMapping("/{productId}")
    public ResponseEntity<String> addImages(
            @RequestBody @NotNull List<MultipartFile> multipartFiles,
            @PathVariable Long productId,
            @AuthenticationPrincipal String shopId) {
        try {
            imageService.addImages(multipartFiles, productId, shopId);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        } catch (ProductNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                "new image of the product was added successfully"
        );
    }

    @PostMapping("/{productId}")
    public ResponseEntity<String> addAvatar (
            @RequestBody @NotNull MultipartFile multipartFile,
            @PathVariable Long productId,
            @AuthenticationPrincipal String shopId) {

        try {
            imageService.addAvatar(multipartFile.getBytes(), productId, shopId);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        } catch (ProductNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                "new avatar was added successfully"
        );
    }
}
