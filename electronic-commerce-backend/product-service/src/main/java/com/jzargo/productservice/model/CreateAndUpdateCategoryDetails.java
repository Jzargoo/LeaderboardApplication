package com.jzargo.productservice.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Validated
public class CreateAndUpdateCategoryDetails {
    @NotNull(message = "The name must contain at least 3 characters")
    private String name;
    private Map<String, String> attributes;
    private Integer parentId;
}
