package com.jzargo.productservice.service;


import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.exception.ShopDoesNotOwnProductException;
import com.jzargo.productservice.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true) // if not provided, data must be in immutable state
public class ImageServiceImpl implements ImageService {


    private final ImageDriver imageDriver;
    private final ProductRepository productRepository;

    public ImageServiceImpl(ImageDriver imageDriver, ProductRepository productRepository) {
        this.imageDriver = imageDriver;
        this.productRepository = productRepository;
    }


    @Override
    @Transactional
    public void addImages(List<MultipartFile> images, Long productId, Long userId)
    throws ProductNotFoundException{

        Product product = productRepository
                .findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (product.getShopId().equals(userId)) {
            throw new ShopDoesNotOwnProductException();
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
    @Transactional
    public void addAvatar(byte[] image, Long productId, Long shopId)
            throws IOException, ProductNotFoundException {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        if (product.getShopId().equals(shopId)) {
            throw new ShopDoesNotOwnProductException();
        }

        String imageName = imageDriver.saveFile(image);

        product.setAvatar(imageName);

        productRepository.save(product);
    }

    public byte[] getAvatar(Long productId)
        throws ProductNotFoundException, IOException {

        String avatar = productRepository
                .findById(productId)
                .map(Product::getAvatar)
                .orElseThrow(ProductNotFoundException::new);

        return imageDriver.getImage(avatar);
    }

    @Override
    @Transactional(readOnly = true)
    public List<byte[]> getAllImages(Long productId)
        throws ProductNotFoundException, IOException {

        List<String> allImages = productRepository
                .findById(productId)
                .map(product -> {
                    List<String> images = product.getImages();
                    images.add(product.getAvatar());
                    return images;
                })
                .orElseThrow(
                        ProductNotFoundException::new
                );

        return imageDriver.getImages(allImages);
    }
}