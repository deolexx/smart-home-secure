package com.smarthome.hub.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	private final RestAuthenticationEntryPoint authenticationEntryPoint;
	private final RestAccessDeniedHandler accessDeniedHandler;

	public SecurityConfig(RestAuthenticationEntryPoint authenticationEntryPoint,
	                      RestAccessDeniedHandler accessDeniedHandler) {
		this.authenticationEntryPoint = authenticationEntryPoint;
		this.accessDeniedHandler = accessDeniedHandler;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health").permitAll()
						.requestMatchers("/api/auth/**").permitAll() // Keycloak handles auth
						.anyRequest().authenticated()
				)
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt
								.jwtAuthenticationConverter(jwtAuthenticationConverter())
						)
				);
		return http.build();
	}

	@Bean
	public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
		return converter;
	}

	@Bean
	public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
		JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
		defaultConverter.setAuthorityPrefix("ROLE_");
		defaultConverter.setAuthoritiesClaimName("realm_access.roles");

		return jwt -> {
			Collection<GrantedAuthority> authorities = defaultConverter.convert(jwt);
			
			// Also extract roles from resource_access if needed
			Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
			if (resourceAccess != null) {
				@SuppressWarnings("unchecked")
				Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("smart-home-hub");
				if (clientAccess != null) {
					@SuppressWarnings("unchecked")
					List<String> roles = (List<String>) clientAccess.get("roles");
					if (roles != null) {
						Collection<GrantedAuthority> clientRoles = roles.stream()
								.map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
								.collect(Collectors.toList());
						return Stream.concat(authorities.stream(), clientRoles.stream())
								.collect(Collectors.toList());
					}
				}
			}
			
			return authorities;
		};
	}
}

