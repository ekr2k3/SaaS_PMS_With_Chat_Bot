package com.ekr.saas_pms_with_chatBot.config;

import com.ekr.saas_pms_with_chatBot.filter.JWTTokenValidatorFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class ProjectSecurityConfig {

    @Autowired
    JWTTokenValidatorFilter JwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/api/v1/saas/user/register",
                        "/api/v1/saas/user/login",
                        "/api/v1/saas/user/Plan/List",
                        "/api/v1/saas/user/Plan/Detail",
                        "/error"
                ).permitAll()
                .requestMatchers(
                        "/api/v1/saas/admin/**"
                ).hasRole("ADMIN")
                .requestMatchers(
                        "/api/v1/user/sendRequest"
                ).hasRole("OWNER")
                .anyRequest().authenticated()
        );
        http.cors(Customizer.withDefaults());   // Bật CORS
        http.csrf(csrf -> csrf.disable());     // Tắt CSRF
        //http.formLogin(withDefaults());
        //http.httpBasic(withDefaults());

        http.addFilterBefore(
                JwtFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
