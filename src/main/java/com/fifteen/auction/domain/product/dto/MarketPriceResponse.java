package com.fifteen.auction.domain.product.dto;

import com.fifteen.auction.domain.product.entity.MarketPrice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class MarketPriceResponse {

    private Long productId;
    private Long minMarketPrice;
    private Long maxMarketPrice;
    private LocalDate priceDate;
    private LocalDateTime createdAt;

    public static MarketPriceResponse fromEntity(MarketPrice marketPrice) {
        return MarketPriceResponse.builder()
                .productId(marketPrice.getProduct().getId())
                .minMarketPrice(marketPrice.getMinMarketPrice())
                .maxMarketPrice(marketPrice.getMaxMarketPrice())
                .priceDate(marketPrice.getPriceDate())
                .createdAt(marketPrice.getCreatedAt())
                .build();
    }
}
