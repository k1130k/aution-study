package com.fifteen.auction.domain.product.controller;

import com.fifteen.auction.domain.product.dto.MarketPriceResponse;
import com.fifteen.auction.domain.product.dto.MarketPriceSummaryResponse;
import com.fifteen.auction.domain.product.dto.TestRequest;
import com.fifteen.auction.domain.product.entity.Product;
import com.fifteen.auction.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
@Profile("local") // 로컬에서만 동작
public class TestController {

    private final ProductService productService;

    @PostMapping("/test-gpt")
    public ResponseEntity<String> testGpt(@RequestBody TestRequest dto) {
        Product product = dto.toEntity();
        productService.createProduct(product);
        return ResponseEntity.ok("GPT 호출 및 시세 예측 완료!");
    }

    @GetMapping("/market-price/full/{productId}")
    public ResponseEntity<MarketPriceSummaryResponse> getFullMarketPrice(@PathVariable Long productId) {
        MarketPriceSummaryResponse response = productService.getFullMarketPriceInfo(productId);
        return ResponseEntity.ok(response);
    }
}