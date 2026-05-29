package com.jzargo.productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    private String sellerId;

    @Builder.Default
    private boolean available = false;


    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> characteristics;

    @ElementCollection
    private List<String> images;

    public void addImage(String imageName){
        images.add(imageName);
    }
}
