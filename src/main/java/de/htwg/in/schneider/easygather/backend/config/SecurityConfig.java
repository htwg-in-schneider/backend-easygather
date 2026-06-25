package de.htwg.in.schneider.easygather.backend.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class SecurityConfig {

    private static boolean isPublicShopRead(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri.equals("/api/product") || uri.startsWith("/api/product/")
                || uri.equals("/api/category") || uri.startsWith("/api/category/");
    }

    private static final RequestMatcher PUBLIC_SHOP_READ = SecurityConfig::isPublicShopRead;

    /**
     * Shop reads without OAuth2/JWT (no BearerTokenAuthenticationFilter on this chain).
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicShopReadChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(PUBLIC_SHOP_READ)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(withDefaults())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securedApiChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(new NegatedRequestMatcher(PUBLIC_SHOP_READ))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/profile").authenticated()
                        .requestMatchers("/api/delivery/**").authenticated()
                        .requestMatchers("/api/order/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/product", "/api/product/*").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/product/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/product/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/category", "/api/category/*").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/category/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/category/*").authenticated()
                        .requestMatchers("/api/user", "/api/user/*").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
                .build();
    }
}
