package com.codehaja.common.config;

import com.codehaja.domain.terminal.InteractiveTerminalHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration(proxyBeanMethods = false)
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final InteractiveTerminalHandler terminalHandler;

    public WebSocketConfig(InteractiveTerminalHandler terminalHandler) {
        this.terminalHandler = terminalHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(terminalHandler, "/ws/terminal")
                .setAllowedOriginPatterns(
                        "http://localhost:*",
                        "http://172.27.*:*",
                        "http://192.168.*:*"
                );
    }
}
