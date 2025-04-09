package com.fifteen.auction.domain.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class MarketPriceFullResponse {
    private MarketPriceResponse todayPrice; // 오늘 시세 (Redis 캐시)
    private List<MarketPriceResponse> historicalPrices; // 최근 3개월 시세 (DB)
}
