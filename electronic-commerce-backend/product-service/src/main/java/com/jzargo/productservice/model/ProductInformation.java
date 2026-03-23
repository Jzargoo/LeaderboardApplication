package com.jzargo.productservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInformation {
    private Long id;
    private String name;
    private Float avgRate;
    private Double price;
    private Long shopId;
}