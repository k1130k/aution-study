package com.fifteen.auction.global.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fifteen.auction.domain.product.dto.GPTPricePredictionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
@Component
@RequiredArgsConstructor
public class OpenAIClient {

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${naver.client.id}")
    private String naverClientId;

    @Value("${naver.client.secret}")
    private String naverClientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<GPTPricePredictionResponse> callGptForHistoricalPrices(String title, String description) {
        System.out.println("GPT 호출용 title: " + title);
        System.out.println("GPT 호출용 description: " + description);
        LocalDate today = LocalDate.now();

        // 날짜 리스트 생성 코드
        List<LocalDate> dates = IntStream.rangeClosed(1, 3)
                .mapToObj(i -> today.minusMonths(i).withDayOfMonth(1))
                .collect(Collectors.toList());
        Collections.reverse(dates);
        dates.add(today);

        String prompt = String.format("""
            다음은 중고 상품 "%s" (설명: %s)에 대한 최근 3개월(각 월의 1일 기준)과 오늘(%s)의 중고 거래 가격 범위를 예측하는 작업이야
            반드시 아래와 같은 tool_call JSON을 정확하게 만들어야 해.
            그 안의 "query"는 무조건 "%s"로 채워야 해. 절대 생략하거나 빈 값 넣지 마.
            형식을 바꾸지도 마. 그대로 써. 복사해서 써.
           
            아래와 똑같이 응답해:
                        
            ```json
            {
             "tool_calls": [
               {
                 "function": {
                   "name": "search_web",
                   "arguments": {
                     "query": "%s"
                  }
                 }
                }
              ]
             } 
                   
            절대 하지 말아야 할 것:
            - "정확한 정보를 제공할 수 없습니다" 같은 문구
            - 실시간 검색 결과만 기반으로 판단해야 해"
            - query는 반드시 "%s"를 사용해. 빈 값 넣지 마. 생략하지 마.
            - 다른 키를 넣거나 구조를 바꾸는 것 절대하지마
            반드시 해야 할 것:
            - 네이버 쇼핑 검색 결과(가격 정보)를 기반으로 추정할 것
            - 각 항목의 "min", "max"는 정수이며 실거래가를 추론한 값일 것
            - "date"는 각 월의 1일 날짜 형식(예: 2025-01-01)
            지금 바로 위 조건에 맞는 JSON 배열을 정확하게 응답해.
           
           """,
                title,
                description,
                today,
                title,
                title,
                title
        );

        Map<String, Object> toolDefinition = Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "search_web",
                        "description", "사용자의 쿼리를 기반으로 실시간 웹 검색을 수행합니다.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "query", Map.of(
                                                "type", "string",
                                                "description", "검색 키워드"
                                        )
                                ),
                                "required", List.of("query")
                        )
                )
        );

        Map<String, Object> request = new HashMap<>();
        request.put("model", "gpt-4-1106-preview");
        request.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        request.put("temperature", 1.0);
        request.put("tools", List.of(toolDefinition));

        Map<String, Object> toolChoice = new HashMap<>();
        toolChoice.put("type", "function");

        Map<String, Object> functionMap = new HashMap<>();
        functionMap.put("name", "search_web");

        Map<String, Object> argumentsMap = new HashMap<>();
        argumentsMap.put("query", title); // title은 상품명

        functionMap.put("arguments", argumentsMap);
        toolChoice.put("function", functionMap);

        request.put("tool_choice", toolChoice);

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());

            // GPT가 tool_call 호출했다고 가정 (이젠 무조건 호출하도록 강제했으니까)
            JsonNode toolCall = root.path("choices").get(0).path("message").path("tool_calls").get(0);
            String argsJson = toolCall.path("function").path("arguments").toString();

            JsonNode argsNode = objectMapper.readTree(argsJson);
            String query = argsNode.path("query").asText();

            if (query == null || query.isBlank()) {
                System.out.println("GPT가 query를 공백처리를 한 관계로 fallback으로 title 사용");
                query = title;
            }

            System.out.println("GPT에게 강제한 검색 쿼리: " + query);

            String searchResult = performSimpleWebSearch(query);
            System.out.println("네이버 검색 결과:\n" + searchResult);

            // follow-up 요청 (tool 응답 포함)
            Map<String, Object> followUpRequest = new HashMap<>();
            followUpRequest.put("model", "gpt-4-1106-preview");
            followUpRequest.put("messages", List.of(
                    Map.of("role", "user", "content", prompt),
                    Map.of("role", "assistant", "tool_calls", List.of(toolCall)),
                    Map.of("role", "tool", "tool_call_id", toolCall.path("id").asText(), "content", searchResult),
                    Map.of("role", "user", "content",
                            """
                            다음 JSON은 실시간 가격 데이터야. 이걸 기반으로 GPTPricePredictionResponse만 가격 범위를 예측하고, 반드시 동일한 형식의 JSON 배열로 응답해.
                            """
                    )
            ));

            HttpEntity<Map<String, Object>> followUpEntity = new HttpEntity<>(followUpRequest, headers);
            ResponseEntity<String> followUpResponse = restTemplate.exchange(apiUrl, HttpMethod.POST, followUpEntity, String.class);

            String content = objectMapper.readTree(followUpResponse.getBody())
                    .path("choices").get(0).path("message").path("content").asText();

            System.out.println("GPT 최종 응답:\n" + content);

            if (content.contains("[") && content.contains("]")) {
                String jsonArrayStr = content.substring(content.indexOf("["), content.lastIndexOf("]") + 1);
                System.out.println("추출된 JSON 배열:\n" + jsonArrayStr);

                return objectMapper.readValue(
                        jsonArrayStr,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, GPTPricePredictionResponse.class)
                );
            } else {
                System.out.println("JSON 배열 형식 아님: " + content);
                return Collections.emptyList();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private String performSimpleWebSearch(String query) {


        String url = "https://openapi.naver.com/v1/search/shop.json?query="
                     + URLEncoder.encode(query, StandardCharsets.UTF_8)
                     + "&display=10";
        System.out.println("네이버 쇼핑 검색 쿼리: " + query);
        System.out.println("요청 URL: " + url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("X-Naver-Client-Id", naverClientId);
        headers.set("X-Naver-Client-Secret", naverClientSecret);
        System.out.println("헤더 체크: " + headers);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);

            System.out.println("네이버 원본 응답: \n" + response.getBody());

            JsonNode items = objectMapper.readTree(response.getBody()).path("items");
            if (!items.isArray() || items.isEmpty()) {
                return "[네이버 쇼핑 가격 데이터]\n검색 결과가 없습니다.";
            }

            ArrayNode pricesArray = objectMapper.createArrayNode();
            for (int i = 0; i < Math.min(items.size(), 5); i++) {
                JsonNode item = items.get(i);
                String itemTitle = item.path("title").asText().replaceAll("<[^>]*>", "");
                int price = item.path("lprice").asInt();

                ObjectNode priceObj = objectMapper.createObjectNode();
                priceObj.put("title", itemTitle);
                priceObj.put("price", price);
                pricesArray.add(priceObj);
            }

            return "[네이버 쇼핑 가격 데이터]\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(pricesArray);

        } catch (Exception e) {
            System.err.println("네이버 쇼핑 검색 실패: " + e.getMessage());
            return "[네이버 쇼핑 가격 데이터]\n'" + query + "' 검색 중 오류 발생";
        }
    }
}

