package com.codehaja.common.config;

import com.codehaja.auth.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/google",
                                "/api/auth/logout",
                                "/api/auth/refresh",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/cms/auth/login",
                                "/api/cms/auth/logout"
                        ).permitAll()
                        // Public read-only access
                        .requestMatchers(HttpMethod.GET, "/api/courses/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/course-categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/sections/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/lectures/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/lecture-item-entries/**").permitAll()
                        // CMS mutations require ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/courses/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/courses/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/courses/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/courses/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/course-categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/course-categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/course-categories/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/sections/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/sections/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/sections/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/sections/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/lectures/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/lectures/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/lectures/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/lectures/**").permitAll()
                        // Authenticated user endpoints
                        .requestMatchers("/api/enrollments/**").authenticated()
                        .requestMatchers("/api/progress/**").authenticated()
                        // Other
                        .requestMatchers("/api/anonymous-users/**").permitAll()
                        .requestMatchers("/api/coding-submissions/**").authenticated()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
