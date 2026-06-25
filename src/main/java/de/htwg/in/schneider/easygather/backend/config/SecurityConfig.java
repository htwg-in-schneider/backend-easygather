package de.htwg.in.schneider.easygather.backend.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
public class SecurityConfig {

    /**
     * Public product/category reads without JWT validation (needed for shop on GitHub Pages).
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicReadChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(new OrRequestMatcher(
                        new AntPathRequestMatcher("/api/product", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/api/product/**", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/api/category", HttpMethod.GET.name()),
                        new AntPathRequestMatcher("/api/category/**", HttpMethod.GET.name())))
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
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
                .build();
    }
}
