package com.smarthome.hub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Фільтр для тестової авторизації через захардкоджений токен.
 * Працює тільки для /api/test/** ендпойнтів.
 */
@Component
public class TestTokenAuthenticationFilter extends OncePerRequestFilter {

	private static final String TEST_TOKEN = "test-token-123";
	private static final String TEST_USERNAME = "test";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		String path = request.getRequestURI();
		
		// Працюємо тільки для тестових ендпойнтів
		if (path.startsWith("/api/test/")) {
			String authHeader = request.getHeader("Authorization");
			
			if (authHeader != null && authHeader.startsWith("Bearer ")) {
				String token = authHeader.substring(7);
				
				// Перевіряємо тестовий токен
				if (TEST_TOKEN.equals(token)) {
					// Створюємо аутентифікацію для тестового користувача
					UsernamePasswordAuthenticationToken authentication = 
							new UsernamePasswordAuthenticationToken(
									TEST_USERNAME,
									null,
									List.of(new SimpleGrantedAuthority("ROLE_USER"))
							);
					
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			}
		}
		
		filterChain.doFilter(request, response);
	}
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		// Пропускаємо фільтр для ендпойнтів, які не потребують авторизації
		String path = request.getRequestURI();
		return !path.startsWith("/api/test/");
	}
}
