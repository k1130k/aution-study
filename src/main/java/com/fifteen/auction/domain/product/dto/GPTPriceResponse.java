package com.fifteen.auction.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GPTPriceResponse {
    private final Long min;
    private final Long max;
}
