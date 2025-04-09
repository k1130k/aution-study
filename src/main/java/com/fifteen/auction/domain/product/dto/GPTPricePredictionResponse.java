package com.fifteen.auction.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GPTPricePredictionResponse {
    private String date;
    private Long min;
    private Long max;
}
