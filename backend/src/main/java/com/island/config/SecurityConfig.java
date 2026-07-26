package com.island.config;

import com.island.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthFilter jwtAuthFilter;
	private final IslandProperties islandProperties;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/v1/auth/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/reading/chapters/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/reading/passages/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/vocabulary/search").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/vocabulary/lookup").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/vocabulary/{id}").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/translation/chapters/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/translation/questions/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/daily/topics").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/daily/hub").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/daily/articles/*").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/sim-exam/hub").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/sim-exam/passages").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/sim-exam/passages/*").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/videos/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/posts/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/files/**").permitAll()
						.requestMatchers("/uploads/**").permitAll()
						.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		var config = new CorsConfiguration();
		config.setAllowedOrigins(islandProperties.getCors().getAllowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);
		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
}
