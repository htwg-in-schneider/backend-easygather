package de.htwg.in.schneider.easygather.backend.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/profile").authenticated()
                        .requestMatchers("/api/order", "/api/order/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/product", "/api/product/*").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/product/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/product/*").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/product", "/api/product/*").permitAll()
                        .requestMatchers("/api/**").permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
                .build();
    }
}
