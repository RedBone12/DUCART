package com.prashant.api.ecom.ducart.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtFilter;
        private final JsonAuthenticationEntryPoint authenticationEntryPoint;
        private final JsonAccessDeniedHandler accessDeniedHandler;
        private final CustomUserDetailsService userDetailsService;

        public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                        JsonAuthenticationEntryPoint authenticationEntryPoint,
                        JsonAccessDeniedHandler accessDeniedHandler,
                        CustomUserDetailsService userDetailsService) {
                this.jwtFilter = jwtFilter;
                this.authenticationEntryPoint = authenticationEntryPoint;
                this.accessDeniedHandler = accessDeniedHandler;
                this.userDetailsService = userDetailsService;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
                provider.setUserDetailsService(userDetailsService);
                provider.setPasswordEncoder(passwordEncoder);
                return provider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of("http://localhost:3000"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http,
                        AuthenticationProvider authenticationProvider) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(Customizer.withDefaults())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider)
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(authenticationEntryPoint)
                                                .accessDeniedHandler(accessDeniedHandler))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/user/login").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/user").permitAll()
                                                .requestMatchers(HttpMethod.PUT, "/user/forgot-password").permitAll()
                                                .requestMatchers("/uploads/**").permitAll()
                                                .requestMatchers(HttpMethod.GET,
                                                                "/product/**", "/brand/**", "/maincategory/**",
                                                                "/subcategory/**", "/testimonial/**")
                                                .permitAll()

                                                .requestMatchers(HttpMethod.POST, "/product/**", "/brand/**",
                                                                "/maincategory/**",
                                                                "/subcategory/**", "/testimonial/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/product/**", "/brand/**",
                                                                "/maincategory/**",
                                                                "/subcategory/**", "/testimonial/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.DELETE, "/product/**", "/brand/**",
                                                                "/maincategory/**",
                                                                "/subcategory/**", "/testimonial/**")
                                                .hasRole("ADMIN")

                                                .requestMatchers("/cart/**", "/wishlist/**", "/checkout/**")
                                                .authenticated()

                                                // .requestMatchers(HttpMethod.GET, "/user").hasRole("ADMIN")
                                                // .requestMatchers(HttpMethod.DELETE, "/user/**").hasRole("ADMIN")
                                                // .requestMatchers(HttpMethod.GET, "/user/**").authenticated()
                                                // .requestMatchers(HttpMethod.PUT, "/user/**").authenticated()

                                                .requestMatchers("/contactus/**", "/newsletter/**").permitAll()

                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
