package com.fifteen.auction.global.client;

import com.fifteen.auction.domain.product.dto.GPTPriceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAIClient {

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public GPTPriceResponseDto callGptForPrice(String prompt) {
        // 1. 요청 본문 구성
        Map<String, Object> request = new HashMap<>();
        request.put("model", "gpt-3.5-turbo");
        request.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        request.put("temperature", 0.7);

        // 2. HTTP 헤더 구성
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // 3. 요청 보내기
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        // 4. 응답 파싱 (간단한 예시)
        String result = response.getBody();

        // TODO: 실제 GPT 응답 파싱 로직 넣기 (여기선 예시 값 리턴)
        Long min = 30000L;
        Long max = 50000L;

        return new GPTPriceResponseDto(min, max);
    }
}
