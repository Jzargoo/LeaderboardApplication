package com.jzargo.productservice.unit;


import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.exception.ProductNotFoundException;
import com.jzargo.productservice.repository.ProductRepository;
import com.jzargo.productservice.service.ImageDriverNative;
import com.jzargo.productservice.service.ImageServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.parameters.P;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class ImageServiceUnitTest {

    static private Long PRODUCT_ID = 1L;
    static private Long SHOP_ID = 1L;

    Product product = Product.builder().build();

    @Mock
    private ImageDriverNative imageDriverNative;

    @InjectMocks
    private ImageServiceImpl imageService;

    @Mock
    private ProductRepository productRepository;

    @BeforeEach
    public void setup(){

        product.setId(PRODUCT_ID);
        product.setShopId(SHOP_ID);

        Mockito.when(
                productRepository.findById(PRODUCT_ID)
        ).thenReturn(Optional.of(product));

    }

    @Test
    public void addImages_test_success(){

        byte[][] contents = {
                {5,4,3,2,1},
                {1,2,3,4,5}
        };

        List<String> images = List.of("image1.png", "image2.png");


        try {
            Mockito.when(
                    imageDriverNative.saveFiles(any(byte[][].class))
            ).thenReturn(images);

            imageService.addImages(contents, PRODUCT_ID, SHOP_ID);

            Assertions.assertEquals(product.getImages(), images);

            Mockito.verify(
                    imageDriverNative,
                    Mockito.times(1)
            ).saveFiles(
                    any(byte[][].class)
            );

            Mockito.verify(
                    productRepository,
                    Mockito.times(1)
            ).findById(PRODUCT_ID);


        } catch (IOException e) {
            Assertions.fail("add images threw io exception. Cannot create files!");
        } catch (ProductNotFoundException e) {
            Assertions.fail("Product was not found in the repository");
        }
    }

    @Test
    public void addAvatar_test_success() {
        byte[] content= {1,2,3};

        var image = "image.png";

        try {
            Mockito.when(
                    imageDriverNative.saveFile(any(byte[].class))
            ).thenReturn(image);

            imageService.addAvatar(content, PRODUCT_ID, SHOP_ID);

            Assertions.assertEquals(image, product.getAvatar());

            Mockito.verify(
                    imageDriverNative,
                    Mockito.times(1)
            ).saveFile(
                    any(byte[].class)
            );

            Mockito.verify(
                    productRepository,
                    Mockito.times(1)
            ).findById(PRODUCT_ID);


        } catch (IOException e) {
            Assertions.fail("add images threw io exception. Cannot create files!");
        } catch (ProductNotFoundException e) {
            Assertions.fail("Product was not found in the repository");
        }
    }

    @Test
    public void getAvatar_test_success(){

        product.setAvatar("image.png");

        byte[] content = {1,2};

        try {

            Mockito.when(
                    imageDriverNative.getImage(product.getAvatar())
            ).thenReturn(content);

            byte[] avatar = imageService.getAvatar(PRODUCT_ID);

            Assertions.assertArrayEquals(content, avatar, "Content of the avatar does not match");

            Mockito.verify(
                    imageDriverNative,
                    Mockito.times(1)
            ).getImage(
                    product.getAvatar()
            );

            Mockito.verify(
                    productRepository,
                    Mockito.times(1)
            ).findById(PRODUCT_ID);


        } catch (IOException e) {
            Assertions.fail("add images threw io exception. Cannot create files!");
        } catch (ProductNotFoundException e) {
            Assertions.fail("Product was not found in the repository");
        }
    }

    @Test
    public void getImages_test_success(){

        List<byte[]> contents = List.of(
                new byte[]{5, 4, 3, 2, 1},
                new byte[]{1, 2, 3, 4, 5}
        );

        List<String> images = List.of("image1.png", "image2.png");

        product.setImages(images);

        try {
            Mockito.when(
                    imageDriverNative.getImages(images)
            ).thenReturn(contents);

            List<byte[]> returnedImages = imageService.getImages(PRODUCT_ID);

            Assertions.assertEquals(
                    contents.size(),
                    returnedImages.size(),
                    "The returned number of images does not match with actual number"
            );

            for (int i = 0; i < returnedImages.size(); i++) {
                Assertions.assertArrayEquals(contents.get(i), returnedImages.get(i));
            }

            Mockito.verify(
                    imageDriverNative,
                    Mockito.times(1)
            ).getImages(
                images
            );

            Mockito.verify(
                    productRepository,
                    Mockito.times(1)
            ).findById(PRODUCT_ID);

        } catch (IOException e) {
            Assertions.fail("add images threw io exception. Cannot create files!");
        } catch (ProductNotFoundException e) {
            Assertions.fail("Product was not found in the repository");
        }
    }
}
