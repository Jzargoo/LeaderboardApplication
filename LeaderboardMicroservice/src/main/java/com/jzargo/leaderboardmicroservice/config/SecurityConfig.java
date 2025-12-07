package com.jzargo.leaderboardmicroservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

@Profile("!standalone")
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .sessionManagement((session) ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(
                        (configurer) ->
                                configurer.jwt(Customizer.withDefaults())
                )
                .authorizeHttpRequests(
                        (router) ->
                            router
                                    .requestMatchers(regexMatcher("/api/v1/[A-Za-z0-9]+")).permitAll()
                                    .requestMatchers("api/v1/create").hasRole("CREATOR")
                                    .anyRequest().authenticated()
                )
                .build();
    }

    @Bean
    @SuppressWarnings("unchecked")
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter jwtAuthenticationConverter =
                new JwtAuthenticationConverter();

        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
                oauth2Token -> {
                    List<String> roles = (List<String>) oauth2Token
                            .getClaimAsMap("realm_access")
                            .get("roles");

                    AbstractAuthenticationToken authorities= jwtAuthenticationConverter.convert(oauth2Token);
                    if (roles == null) {
                        return Collections.emptyList();
                    }
                    return Stream
                            .concat(authorities.getAuthorities().stream(),
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
