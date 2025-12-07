package com.jzargo.gatewayapi.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.*;
import org.springframework.security.web.server.header.ClearSiteDataServerHttpHeadersWriter;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;


@Configuration
@EnableWebFluxSecurity
@EnableRedisWebSession
public class SecurityConfig {

    @Value("${app.redirect-uri}")
    private String redirectUri;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ServerOAuth2AuthorizationRequestResolver requestResolver,
            ServerOAuth2AuthorizedClientRepository repository,
            ServerLogoutHandler handler,
            ServerLogoutSuccessHandler successHandler
            ) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .oauth2Login(oath2 -> oath2
                        .authorizationRequestResolver(requestResolver)
                        .authorizedClientRepository(repository)
                )
                .logout(logoutSpec -> logoutSpec
                        .logoutHandler(handler)
                        .logoutSuccessHandler(successHandler)
                )
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/leaderboard/view/**").permitAll()
                        .anyExchange().authenticated()
                );
        return http.build();
    }

    @Bean
    ServerOAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ReactiveClientRegistrationRepository clientRegistrationRepository
    ) {
        DefaultServerOAuth2AuthorizationRequestResolver defaultServerOAuth2AuthorizationRequestResolver =
                new DefaultServerOAuth2AuthorizationRequestResolver(clientRegistrationRepository);

        defaultServerOAuth2AuthorizationRequestResolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers
                .withPkce());
        return defaultServerOAuth2AuthorizationRequestResolver;
    }

    @Bean
    public  ServerOAuth2AuthorizedClientRepository serverOAuth2AuthorizedClientRepository(){
        return new WebSessionServerOAuth2AuthorizedClientRepository();
    }

    @Bean
    public ServerLogoutHandler serverLogoutHandler() {
        return new DelegatingServerLogoutHandler(
                new SecurityContextServerLogoutHandler(),
                new WebSessionServerLogoutHandler(),
                new HeaderWriterServerLogoutHandler(
                        new ClearSiteDataServerHttpHeadersWriter(
                                ClearSiteDataServerHttpHeadersWriter.Directive.COOKIES
                        )
                )
        );
    }

    @Bean
    ServerLogoutSuccessHandler logoutSuccessHandler(ReactiveClientRegistrationRepository clientRegistrationRepository) {
        OidcClientInitiatedServerLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);
        oidcLogoutSuccessHandler.setPostLogoutRedirectUri(redirectUri);
        return oidcLogoutSuccessHandler;
    }

}