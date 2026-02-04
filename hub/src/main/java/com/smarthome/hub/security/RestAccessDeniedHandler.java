package com.smarthome.hub.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RestAccessDeniedHandler implements AccessDeniedHandler {
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
		logAccessDenied(request);
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		new ObjectMapper().writeValue(response.getOutputStream(), Map.of(
				"error", "forbidden",
				"message", accessDeniedException.getMessage()
		));
	}

	private void logAccessDenied(HttpServletRequest request) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) {
			log.warn("Access denied: {} {} (no authentication)", request.getMethod(), request.getRequestURI());
			return;
		}

		String authorities = auth.getAuthorities().stream()
				.map(a -> a.getAuthority())
				.sorted()
				.collect(Collectors.joining(","));
		boolean hasAdmin = auth.getAuthorities().stream()
				.anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

		log.warn("Access denied: {} {} user={} authorities=[{}] roleAdmin={}",
				request.getMethod(),
				request.getRequestURI(),
				auth.getName(),
				authorities,
				hasAdmin);
	}
}

