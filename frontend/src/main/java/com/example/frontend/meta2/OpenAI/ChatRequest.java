package com.example.frontend.meta2.OpenAI;

import java.util.List;

public record ChatRequest(String model, List<ChatMessage> messages) {
}