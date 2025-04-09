package com.fifteen.auction.domain.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fifteen.auction.domain.product.dto.GPTPricePredictionResponse;
import com.fifteen.auction.domain.product.dto.MarketPriceResponse;
import com.fifteen.auction.domain.product.dto.MarketPriceFullResponse;
import com.fifteen.auction.domain.product.entity.MarketPrice;
import com.fifteen.auction.domain.product.entity.Product;
import com.fifteen.auction.domain.product.repository.MarketPriceRepository;
import com.fifteen.auction.domain.product.repository.ProductRepository;
import com.fifteen.auction.global.client.OpenAIClient;
import com.fifteen.auction.global.dto.error.ErrorCode;
import com.fifteen.auction.global.dto.exception.ServerException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MarketPriceService {

    private final ProductRepository productRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final OpenAIClient openAIClient;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "price:";
    private static final long TTL_HOURS = 24L;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // 상품 등록 + GPT 시세 예측 및 저장
    @Transactional
    public Product createProduct(Product product) {
        Product savedProduct = productRepository.save(product);
        predictAndSavePrice(savedProduct); // GPT 호출 → 시세 저장
        return savedProduct;
    }

    //GPT로 시세 예측 → 최근 3개월 시세는 DB, 오늘은 Redis 캐시에 저장
    public void predictAndSavePrice(Product product) {
        List<GPTPricePredictionResponse> historicalPrices = openAIClient.callGptForHistoricalPrices(
                product.getTitle(),
                product.getDescription()
        );

        Set<LocalDate> savedDates = new HashSet<>();

        for (int i = 0; i < historicalPrices.size(); i++) {
            GPTPricePredictionResponse dto = historicalPrices.get(i);
            LocalDate priceDate = LocalDate.parse(dto.getDate());

            //  GPT 응답 내 중복 날짜 방지
            if (!savedDates.add(priceDate)) continue;

            boolean isToday = (i == historicalPrices.size() - 1);
            boolean alreadySaved = marketPriceRepository.existsByProductIdAndPriceDate(product.getId(), priceDate);

            if (!isToday && alreadySaved) continue;

            MarketPrice price = MarketPrice.builder()
                    .product(product)
                    .priceDate(priceDate)
                    .minMarketPrice(dto.getMin())
                    .maxMarketPrice(dto.getMax())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            if (isToday) {
                redisTemplate.opsForValue().set(
                        CACHE_PREFIX + product.getId(),
                        price,
                        TTL_HOURS,
                        TimeUnit.HOURS
                );
            } else {
                marketPriceRepository.save(price);
            }
        }
    }


    //오늘 + 최근 3개월 시세조회
    @Transactional(readOnly = true)
    public MarketPriceFullResponse getMarketPriceFullResponse(Long productId) {
        // 1. 오늘 시세 (Redis)
        Object raw = redisTemplate.opsForValue().get(CACHE_PREFIX + productId);
        MarketPrice todayEntity = null;

        if (raw instanceof LinkedHashMap map) {
            todayEntity = objectMapper.convertValue(map, MarketPrice.class);
        } else if (raw instanceof MarketPrice cached) {
            todayEntity = cached;
        }

        if (todayEntity == null) {
            throw new ServerException(ErrorCode.MARKET_PRICE_NOT_FOUND);
        }

        MarketPriceResponse today = MarketPriceResponse.fromEntity(todayEntity);

        // 2. DB에서 최근 3개월 시세
        List<MarketPriceResponse> history = marketPriceRepository
                .findAllByProductIdOrderByPriceDateAsc(productId)
                .stream()
                .map(MarketPriceResponse::fromEntity)
                .toList();

        return MarketPriceFullResponse.builder()
                .todayPrice(today)
                .historicalPrices(history)
                .build();
    }
}