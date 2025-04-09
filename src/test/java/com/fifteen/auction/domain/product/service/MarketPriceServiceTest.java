package com.fifteen.auction.domain.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fifteen.auction.AuctionApplication;
import com.fifteen.auction.domain.product.entity.MarketPrice;
import com.fifteen.auction.domain.product.entity.Product;
import com.fifteen.auction.domain.product.repository.MarketPriceRepository;
import com.fifteen.auction.domain.product.repository.ProductRepository;
import com.fifteen.auction.global.client.OpenAIClient;
import com.fifteen.auction.global.config.RedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {AuctionApplication.class, RedisConfig.class})
class MarketPriceServiceTest {

    @Autowired
    private MarketPriceService marketPriceService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MarketPriceRepository marketPriceRepository;

    @Autowired
    private OpenAIClient openAIClient;

    @Test
    void 상품등록시_GPT시세예측_DB_레디스_정상동작() {
        // given
        Product product = Product.builder()
                .title("아이폰 13 미니")
                .description("생활 기스 약간 있음")
                .build();

        GPTPriceResponse gptPriceResponse = new GPTPriceResponse(30000L, 50000L);

        // when
        Product savedProduct = marketPriceService.createProduct(product);
        String cacheKey = "price:" + savedProduct.getId();

        // then
        // 1. DB에 저장되었는지 확인
        MarketPrice savedPrice = marketPriceRepository.findTopByProductIdOrderByCreatedAtDesc(savedProduct.getId())
                .orElse(null);

        assertNotNull(savedPrice);
        assertEquals(30000L, savedPrice.getMinMarketPrice());
        assertEquals(50000L, savedPrice.getMaxMarketPrice());

        // 2. Redis에도 저장되었는지 확인
        Object raw = redisTemplate.opsForValue().get(cacheKey);

        assertNotNull(raw);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        MarketPrice cachedPrice = objectMapper.convertValue(raw, MarketPrice.class);

        assertNotNull(cachedPrice);
        assertEquals(30000L, cachedPrice.getMinMarketPrice());
        assertEquals(50000L, cachedPrice.getMaxMarketPrice());
    }
}
