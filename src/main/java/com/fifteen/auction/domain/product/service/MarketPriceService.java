package com.fifteen.auction.domain.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fifteen.auction.domain.product.dto.GPTPricePredictionResponse;
import com.fifteen.auction.domain.product.dto.MarketPriceFullResponse;
import com.fifteen.auction.domain.product.dto.MarketPriceResponse;
import com.fifteen.auction.domain.product.entity.MarketPrice;
import com.fifteen.auction.domain.product.entity.Product;
import com.fifteen.auction.domain.product.repository.MarketPriceRepository;
import com.fifteen.auction.domain.product.repository.ProductRepository;
import com.fifteen.auction.global.client.OpenAIClient;
import com.fifteen.auction.global.dto.error.ErrorCode;
import com.fifteen.auction.global.dto.exception.ServerException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MarketPriceService {

    private final MarketPriceRepository marketPriceRepository;
    private final ProductRepository productRepository;
    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Cacheable(value = "marketPrice", key = "#productId")
    @Transactional
    public MarketPriceFullResponse findMarketPriceFullResponse(Long productId) {
        System.out.println("Redis 캐시 만료! GPT호출 & DB 저장!");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ServerException(ErrorCode.PRODUCT_NOT_FOUND));

        // GPT 호출을 통해 오늘 포함 최근 3개월 시세 예측
        List<GPTPricePredictionResponse> predictedPrices = openAIClient.callGptForHistoricalPrices(
                product.getName(),
                product.getDescription()
        );

        Set<LocalDate> savedDates = new HashSet<>();
        MarketPrice todayPrice = null;

        for (int i = 0; i < predictedPrices.size(); i++) {
            GPTPricePredictionResponse dto = predictedPrices.get(i);
            LocalDate priceDate = LocalDate.parse(dto.getDate());

            // 중복 날짜 필터링
            if (!savedDates.add(priceDate)) continue;

            boolean isToday = (i == predictedPrices.size() - 1);
            boolean alreadySaved = marketPriceRepository.existsByProductIdAndPriceDate(productId, priceDate);

            if (!isToday && alreadySaved) continue;

            MarketPrice price = MarketPrice.builder()
                    .product(product)
                    .priceDate(priceDate)
                    .minMarketPrice(dto.getMin())
                    .maxMarketPrice(dto.getMax())
                    .build();

            if (isToday) {
                todayPrice = price;
            } else {
                marketPriceRepository.save(price);
            }
        }

        if (todayPrice == null) {
            throw new ServerException(ErrorCode.MARKET_PRICE_NOT_FOUND);
        }

        // DB에 저장된 최근 3개월 시세 조회 및 DTO 반환
        List<MarketPriceResponse> historicalPrices = marketPriceRepository
                .findAllByProductIdOrderByPriceDateAsc(productId)
                .stream()
                .map(MarketPriceResponse::fromEntity)
                .toList();

        return MarketPriceFullResponse.builder()
                .todayPrice(MarketPriceResponse.fromEntity(todayPrice))
                .historicalPrices(historicalPrices)
                .build();
    }
}
