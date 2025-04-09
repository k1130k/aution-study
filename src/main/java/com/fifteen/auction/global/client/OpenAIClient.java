package com.fifteen.auction.global.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fifteen.auction.domain.product.dto.GPTPricePredictionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class OpenAIClient {

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<GPTPricePredictionResponse> callGptForHistoricalPrices(String title, String description) {
        LocalDate today = LocalDate.now();
        List<LocalDate> dates = IntStream.rangeClosed(1, 3)
                .mapToObj(i -> today.minusMonths(i).withDayOfMonth(1))
                .collect(Collectors.toList());
        Collections.reverse(dates);
        dates.add(today); // 오늘 날짜 포함

        StringBuilder exampleBuilder = new StringBuilder("[\n");
        for (LocalDate date : dates) {
            exampleBuilder.append(String.format("  { \"date\": \"%s\", \"min\": 30000, \"max\": 50000 },\n",
                    date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))));
        }
        exampleBuilder.setLength(exampleBuilder.length() - 2);
        exampleBuilder.append("\n]");

        String prompt = String.format("""
            다음 중고 상품의 최근 3개월 (1일 기준)과 오늘(%s)의 예상 거래 가격 범위를 알려줘.
            형식은 다음과 같이 JSON 배열로 정확하게 반환해줘:

            %s

            제품명: %s
            설명: %s
            참고로 중고나라나 번개장터의 일반적인 거래 기준으로 알려줘.
            """,
                today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                exampleBuilder,
                title,
                description
        );

        Map<String, Object> request = new HashMap<>();
        request.put("model", "gpt-3.5-turbo");
        request.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        request.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            return objectMapper.readValue(
                    content,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, GPTPricePredictionResponse.class)
            );

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}

