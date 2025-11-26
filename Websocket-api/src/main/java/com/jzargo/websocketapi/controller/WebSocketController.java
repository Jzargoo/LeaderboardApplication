package com.jzargo.websocketapi.controller;

import com.jzargo.websocketapi.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketController {
    private final WebSocketService webSocketService;
}
