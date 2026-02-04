package com.smarthome.hub.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

/**
 * Кастомний BearerTokenResolver, який ігнорує тестовий токен
 * та не передає його в OAuth2 Resource Server, якщо вже є аутентифікація.
 */
public class TestBearerTokenResolver implements BearerTokenResolver {

	private static final String TEST_TOKEN = "test-token-123";

	@Override
	public String resolve(HttpServletRequest request) {
		String path = request.getRequestURI();
		
		// Для тестових ендпойнтів
		if (path.startsWith("/api/test/")) {
			// Якщо вже є аутентифікація (встановлена тестовим фільтром), не передаємо токен в OAuth2
			if (SecurityContextHolder.getContext().getAuthentication() != null &&
				SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
				return null;
			}
			
			// Перевіряємо, чи це тестовий токен
			String authHeader = request.getHeader("Authorization");
			if (authHeader != null && authHeader.startsWith("Bearer ")) {
				String token = authHeader.substring(7);
				if (TEST_TOKEN.equals(token)) {
					return null; // Не передаємо тестовий токен в OAuth2
				}
			}
		}
		
		// Для інших ендпойнтів використовуємо стандартну логіку
		String authHeader = request.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);
			// Якщо це тестовий токен, не передаємо його в OAuth2
			if (TEST_TOKEN.equals(token)) {
				return null;
			}
			return token;
		}
		
		return null;
	}
}
