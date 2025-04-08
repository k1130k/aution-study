package com.fifteen.auction.domain.product.repository;

import com.fifteen.auction.domain.product.entity.MarketPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MarketPriceRepository extends JpaRepository<MarketPrice, Long> {

    // 특정 상품의 특정 날짜 시세가 이미 존재하는지 확인
    boolean existsByProductIdAndPriceDate(Long productId, LocalDate marketDate);

    // 특정 상품의 최근 3개월 시세 전체 조회
    List<MarketPrice> findAllByProductIdOrderByPriceDateAsc(Long productId);
}
