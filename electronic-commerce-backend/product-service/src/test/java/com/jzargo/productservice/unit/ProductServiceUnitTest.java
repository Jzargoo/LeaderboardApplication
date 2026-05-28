package com.jzargo.productservice.unit;


import com.jzargo.productservice.entity.Category;
import com.jzargo.productservice.entity.Product;
import com.jzargo.productservice.mapper.ReadProductDetailsMapper;
import com.jzargo.productservice.model.ProductDetails;
import com.jzargo.productservice.repository.ProductRepository;
import com.jzargo.productservice.service.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class ProductServiceUnitTest {
    private final long PRODUCT_ID = 1L;
    private final String SHOP_ID = "shop123";

    private final Product PRODUCT = Product.builder()
            .id(PRODUCT_ID)
            .name("Test product")
            .category(
                    Category.builder()
                            .id(2)
                            .attributes(new HashMap<>())
                            .name("Test category")
                            .build()
            )
            .shopId(SHOP_ID)
            .description("Test desc")
            .stockPrice(14.2)
            .characteristics(new HashMap<>())
            .build();

    @InjectMocks
    public ProductServiceImpl productService;
    @Spy
    public ReadProductDetailsMapper readProductDetailsMapper;
    @Mock
    private ProductRepository productRepository;

    @Test
    public void findById_successTest() {
        Mockito.when(
                productRepository.findById(PRODUCT_ID)
        ).thenReturn(
                Optional.of(PRODUCT)
        );

        ProductDetails productById = productService.getProductById(PRODUCT_ID);

        assertEquals(
                productById.getName(),
                PRODUCT.getName(),
                "Names of both products must match"
        );

        assertEquals(
                productById.getCategory(),
                PRODUCT.getCategory().getName(),
                "Names of both categories must match"
        );

        assertEquals(productById.getCharacteristics(), PRODUCT.getCharacteristics(), "Characteristics of both products must match");

        assertEquals(productById.getDescription(), PRODUCT.getDescription(), "Description of both products must match");

        Mockito.verify(readProductDetailsMapper, Mockito.times(1)).map(PRODUCT);
    }


}
