package com.jzargo.websocketapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class WebsocketApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebsocketApiApplication.class, args);
    }

}
