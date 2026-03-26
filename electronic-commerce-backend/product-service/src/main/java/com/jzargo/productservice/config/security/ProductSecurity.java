package com.jzargo.productservice.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Configuration
public class ProductSecurity {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) {
        return http
                .authorizeHttpRequests( request -> request
                        .anyRequest().permitAll()
                )
                .csrf(CsrfConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth-> oauth.jwt(Customizer.withDefaults()))
                .cors(corsConfigurer -> corsConfigurer.configurationSource(corsConfigurationSource))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration config = new CorsConfiguration();

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addAllowedOrigin("*");

        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource =
                new UrlBasedCorsConfigurationSource();

        urlBasedCorsConfigurationSource.registerCorsConfiguration("/api/**", config);

        return urlBasedCorsConfigurationSource;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(){

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

        jwtAuthenticationConverter.setPrincipalClaimName("user_id");

        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
                jwt -> {
                    Collection<GrantedAuthority> authorities= jwtGrantedAuthoritiesConverter.convert(jwt);

                    List<String>  roles = (List<String>) jwt.getClaimAsMap("realm_access").getOrDefault("roles", List.of());

                    return Stream
                            .concat(
                                    authorities.stream(),
                                    roles.stream()
                                            .filter(role -> role.startsWith("ROLE_"))
                                            .map(SimpleGrantedAuthority::new)
                                            .map(GrantedAuthority.class::cast)
                            ).toList();
                }
        );
        return jwtAuthenticationConverter;
    }
}
