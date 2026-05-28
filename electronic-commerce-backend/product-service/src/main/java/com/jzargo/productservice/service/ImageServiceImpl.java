package com.jzargo.productservice.service;


import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class ImageServiceImpl implements ImageService {


    private final ImageDriver imageDriver;
    private final ProductRepository productRepository;

    public ImageServiceImpl(ImageDriver imageDriver, ProductRepository productRepository) {
        this.imageDriver = imageDriver;
        this.productRepository = productRepository;
    }

    @SneakyThrows
    @Transactional
    @Override
    public void addImages(List<MultipartFile> images, Long productId, String userId)  {

        Product product = productRepository.findById(productId).orElseThrow(
                () ->
                        new ProductNotFoundException(
                                "Cannot find a product with id " +
                                        productId +
                                        " while added a new image")
        );

        if (product.getShopId().equals(userId)) {
            throw new ShopDoesNotOwnProductException(
                    "The shop's id " +
                            productId +
                            " and provided in a product does not match " +
                            product.getShopId()
            );
        }

        try {

            for (MultipartFile image: images) {
                String imageName = imageDriver.saveFile(image.getBytes());
                product.addImage(imageName);
            }

        } catch (IOException e) {
            log.error("Cannot save all images. Unexpected error occurred", e);
        }

        productRepository.save(product);
    }

    @Override
    public void addAvatar(byte[] image, Long productId, String shopId)
            throws IOException, ProductNotFoundException {

        Product product = productRepository.findById(productId).orElseThrow(
                () ->
                        new ProductNotFoundException(
                                "Cannot find a product with id " +
                                        productId +
                                        " while added a new image")
        );

        if (product.getShopId().equals(shopId)) {
            throw new ShopDoesNotOwnProductException(
                    "The shop's id " +
                            productId +
                            " and provided in a product does not match " +
                            product.getShopId()
            );
        }

        String imageName = imageDriver.saveFile(image);

        product.setAvatar(imageName);

        productRepository.save(product);
    }
}