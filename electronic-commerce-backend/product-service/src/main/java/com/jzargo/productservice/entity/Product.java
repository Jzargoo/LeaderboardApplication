package com.jzargo.productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Data
@Table
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String avatar;

    @ManyToOne
    private Category category;
    private String name;
    private String description;
    private Double stockPrice;
    private Long shopId;

    @Builder.Default
    private boolean available = false;


    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, String> characteristics = Map.of();

    @ElementCollection
    @Builder.Default
    private List<String> images = new ArrayList<>();

    public void addImages(List<String> imageNames) {
        images.addAll(imageNames);
    }
    public void addImage(String imageName){
        images.add(imageName);
    }
}
