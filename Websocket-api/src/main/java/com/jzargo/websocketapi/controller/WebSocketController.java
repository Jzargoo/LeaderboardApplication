package com.jzargo.websocketapi.controller;

import com.jzargo.websocketapi.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.socket.WebSocketSession;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class WebSocketController {
    private final WebSocketService webSocketService;

    @MessageMapping("/query/join")
    public void joinToLeaderboard(
            @RequestBody String id,
            Principal principal,
            WebSocketSession webSocketSession
            ){
        webSocketService.initLeaderboardScore(id);
    }
}
