package com.fifteen.auction.domain.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fifteen.auction.domain.product.dto.GPTPriceResponseDto;
import com.fifteen.auction.domain.product.dto.MarketPriceResponseDto;
import com.fifteen.auction.domain.product.entity.MarketPrice;
import com.fifteen.auction.domain.product.entity.Product;
import com.fifteen.auction.domain.product.repository.MarketPriceRepository;
import com.fifteen.auction.domain.product.repository.ProductRepository;
import com.fifteen.auction.global.client.OpenAIClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final OpenAIClient openAIClient;


    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "price:";
    private static final long TTL_HOURS = 24L;

    public Product createProduct(Product product) {
        Product savedProduct = productRepository.save(product);
        predictAndSavePrice(savedProduct); // 상품 등록 후 시세 예측
        return savedProduct;
    }

    public void predictAndSavePrice(Product product) {
        String prompt = String.format(
                "다음 중고 상품의 예상 거래 가격 범위를 알려줘.\n" +
                        "제품명: %s\n" +
                        "설명: %s\n" +
                        "상태: %s\n" +
                        "구성품: %s\n" +
                        "참고로 중고나라나 번개장터의 일반적인 거래 기준으로 알려줘.",
                product.getTitle(),
                product.getDescription(),
                product.getCondition(),
                product.getIncludedItems()
        );

        GPTPriceResponseDto gptResponse = openAIClient.callGptForPrice(prompt);

        MarketPrice price = MarketPrice.builder()
                .product(product)
                .minMarketPrice(gptResponse.getMin())
                .maxMarketPrice(gptResponse.getMax())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        marketPriceRepository.save(price);

        String cacheKey = CACHE_PREFIX + product.getId();
        redisTemplate.opsForValue().set(cacheKey, price, TTL_HOURS, TimeUnit.HOURS);
    }

    public MarketPriceResponseDto getMarketPrice(Long productId) {
        String cacheKey = CACHE_PREFIX + productId;

        Object raw = redisTemplate.opsForValue().get(cacheKey);

        if (raw instanceof LinkedHashMap map) {
            // 여기서 안전하게 변환
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            MarketPrice cached = objectMapper.convertValue(map, MarketPrice.class);
            return MarketPriceResponseDto.from(cached);
        }

        if (raw instanceof MarketPrice cached) {
            // 혹시나 직렬화 없이 객체 자체가 들어있는 경우
            return MarketPriceResponseDto.from(cached);
        }

        // 캐시에 없으면 DB에서 가져오고 다시 캐시에 저장
        MarketPrice price = marketPriceRepository.findTopByProductIdOrderByCreatedAtDesc(productId)
                .orElseThrow(() -> new RuntimeException("시세 정보 없음"));

        redisTemplate.opsForValue().set(cacheKey, price, TTL_HOURS, TimeUnit.HOURS);
        return MarketPriceResponseDto.from(price);
    }
}