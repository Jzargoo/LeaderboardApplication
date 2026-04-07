package com.jzargo.productservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateAndUpdateCategoryDetails {
    private String name;
    private Map<String, String> attributes;
    private Integer parentId;
}
