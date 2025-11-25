package com.example.frontend.meta2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketPushController {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public WebSocketPushController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendToClients(String message) {
        messagingTemplate.convertAndSend("/topic/top10", message);
    }
}
