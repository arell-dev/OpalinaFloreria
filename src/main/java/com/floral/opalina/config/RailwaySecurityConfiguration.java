package com.floral.opalina.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class RailwaySecurityConfiguration {

    @Bean
    @Profile("railway")
    UserDetailsService railwayDemoUserDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${DEMO_ACCESS_USERNAME}") String username,
            @Value("${DEMO_ACCESS_PASSWORD}") String password) {
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Configura las credenciales de acceso de la demostración en Railway.");
        }

        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("DEMO")
                .build());
    }

    @Bean
    @Profile("railway")
    SecurityFilterChain railwaySecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health/readiness").permitAll()
                        .requestMatchers("/api/**").denyAll()
                        .requestMatchers(HttpMethod.POST, "/admin/productos", "/admin/productos/**").denyAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    @Profile("!railway")
    SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .build();
    }
}
