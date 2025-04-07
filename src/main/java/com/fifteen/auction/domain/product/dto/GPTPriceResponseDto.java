package com.fifteen.auction.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GPTPriceResponseDto {
    private final Long min;
    private final Long max;
}
