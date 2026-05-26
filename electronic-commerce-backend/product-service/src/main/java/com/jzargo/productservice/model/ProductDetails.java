package com.jzargo.productservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ProductDetails {
    private String category;
    private String name;
    private Float avgRate;
    private Double price;
    private String description;
    private Map<String, String> characteristics;
}
