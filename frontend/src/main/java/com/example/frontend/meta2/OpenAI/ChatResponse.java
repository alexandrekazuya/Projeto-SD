package com.example.frontend.meta2.OpenAI;

import java.util.List;

public record ChatResponse(List<Choice> choices) {
    public record Choice(Message message) {
        public record Message(String role, String content) {
        }
    }
}