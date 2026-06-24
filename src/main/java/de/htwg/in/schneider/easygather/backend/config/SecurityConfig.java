package de.htwg.in.schneider.easygather.backend.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
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
                        .requestMatchers(HttpMethod.GET, "/api/product", "/api/product/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/category", "/api/category/*").permitAll()
                        .requestMatchers("/api/**").permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
                .build();
    }
}
