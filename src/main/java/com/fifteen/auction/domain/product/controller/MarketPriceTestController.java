package com.fifteen.auction.domain.product.controller;

import com.fifteen.auction.domain.product.dto.MarketPriceFullResponse;
import com.fifteen.auction.domain.product.dto.TestRequest;
import com.fifteen.auction.domain.product.entity.Product;
import com.fifteen.auction.domain.product.entity.ProductCategory;
import com.fifteen.auction.domain.product.repository.ProductCategoryRepository;
import com.fifteen.auction.domain.product.repository.ProductRepository;
import com.fifteen.auction.domain.product.service.MarketPriceService;
import com.fifteen.auction.domain.user.User;
import com.fifteen.auction.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/market-price")
@RequiredArgsConstructor
public class MarketPriceTestController {

    private final MarketPriceService marketPriceService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductCategoryRepository categoryRepository;

    @GetMapping("/{productId}")
    public MarketPriceFullResponse getMarketPrice(@PathVariable Long productId) {
        return marketPriceService.findMarketPriceFullResponse(productId);
    }

    // 테스트용 상품 생성
    @PostMapping("/product")
    public Long createTestProduct() {
        // seller, category 임시로 생성
        User user = userRepository.findByEmail("test@test.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("test@test.com")
                        .nickname("김정은")
                        .preferCategory("전자제품")
                        .build()));

        ProductCategory category = categoryRepository.save(ProductCategory.builder()
                .name("전자제품")
                .build());

        Product product = Product.builder()
                .seller(user)
                .category(category)
                .name("아이폰16프로")
                .description("256기가 미개봉")
                .thumbnailUrl("https://example.com/thumb.jpg")
                .imageUrls(List.of("https://example.com/image1.jpg", "https://example.com/image2.jpg"))
                .build();

        productRepository.save(product);

        return product.getId(); // 생성된 productId 반환
    }
}