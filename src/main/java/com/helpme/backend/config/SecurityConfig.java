package com.helpme.backend.config;

import com.helpme.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/v1/auth/send-otp").permitAll()
                        .requestMatchers("/v1/auth/verify-otp").permitAll()
                        .requestMatchers("/v1/auth/resend-otp").permitAll()
                        .requestMatchers("/v1/auth/check-phone").permitAll()
                        .requestMatchers("/v1/files/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/v1/health").permitAll()
                        .requestMatchers("/error").permitAll()

                        // ✅ Public ratings (driver có thể xem rating trước khi chọn provider)
                        .requestMatchers("/v1/ratings/providers/**").permitAll()

                        // ✅ /v1/auth/me CẦN AUTHENTICATION
                        .requestMatchers("/v1/auth/me").authenticated()

                        // Role-based access
                        .requestMatchers("/v1/driver/**").authenticated()
                        .requestMatchers("/v1/provider/**").hasRole("PROVIDER")

                        // All other requests need authentication
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // ✅ Add exception handling
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter()
                                    .write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
                        }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}