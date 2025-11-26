package com.jzargo.websocketapi.lifecylce;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

@Component
public class FeignRequestInterceptor implements RequestInterceptor{
    private final ThreadLocal<String> jwtHolder = new ThreadLocal<>();

    public void setJwt(String jwt){
        jwtHolder.set(jwt);
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
        String jwt = jwtHolder.get();
        if(jwt != null) {
            requestTemplate.header("Authorization", "Bearer " + jwt);
        }
    }
}
