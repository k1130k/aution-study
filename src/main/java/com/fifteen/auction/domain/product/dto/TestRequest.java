package com.fifteen.auction.domain.product.dto;

import com.fifteen.auction.domain.product.entity.Product;
import lombok.Getter;

@Getter
public class TestRequest {
    private String title;
    private String description;

    public Product toEntity() {
        return Product.builder()
                .title(title)
                .description(description)
                .build();
    }
}
