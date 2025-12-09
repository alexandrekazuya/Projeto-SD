package com.example.frontend.meta2.OpenAI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Service
public class OpenAIService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl = "https://api.groq.com/openai/v1/chat/completions";

    public OpenAIService(@Value("${openai.api.key:}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
    }

    public String getSummary(String query) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            ChatMessage userMessage = new ChatMessage("user", "Tell me about: " + query + " in 100 words");

            ChatRequest requestBody = new ChatRequest("llama-3.1-8b-instant", List.of(userMessage));

            HttpEntity<ChatRequest> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<ChatResponse> response = restTemplate.postForEntity(apiUrl, entity, ChatResponse.class);

            ChatResponse body = response.getBody();
            if (body != null &&
                    body.choices() != null &&
                    !body.choices().isEmpty() &&
                    body.choices().get(0).message() != null) {

                return body.choices().get(0).message().content();
            }

            return "No AI answer";
        } catch (Exception e) {
            e.printStackTrace();
            return "error + " + e.getMessage();
        }
    }
}
