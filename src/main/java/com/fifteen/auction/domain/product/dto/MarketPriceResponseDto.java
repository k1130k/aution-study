package com.fifteen.auction.domain.product.dto;

import com.fifteen.auction.domain.product.entity.MarketPrice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class MarketPriceResponseDto {

    private Long productId;
    private Long minMarketPrice;
    private Long maxMarketPrice;
    private LocalDateTime createdAt;

    public static MarketPriceResponseDto from(MarketPrice marketPrice) {
        return MarketPriceResponseDto.builder()
                .productId(marketPrice.getProduct().getId())
                .minMarketPrice(marketPrice.getMinMarketPrice())
                .maxMarketPrice(marketPrice.getMaxMarketPrice())
                .createdAt(marketPrice.getCreatedAt())
                .build();
    }
}
