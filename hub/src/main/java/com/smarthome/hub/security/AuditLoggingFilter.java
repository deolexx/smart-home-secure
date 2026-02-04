package com.smarthome.hub.security;

import com.smarthome.hub.domain.AuditLog;
import com.smarthome.hub.service.AuditLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AuditLoggingFilter extends OncePerRequestFilter {

	private final AuditLogService auditLogService;

	public AuditLoggingFilter(AuditLogService auditLogService) {
		this.auditLogService = auditLogService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Instant start = Instant.now();
		try {
			filterChain.doFilter(request, response);
		} finally {
			saveAuditLog(request, response, start);
		}
	}

	private void saveAuditLog(HttpServletRequest request, HttpServletResponse response, Instant start) {
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			String userId = null;
			String username = null;
			String roles = null;
			if (auth != null) {
				username = auth.getName();
				if (auth instanceof JwtAuthenticationToken jwtAuth) {
					userId = jwtAuth.getToken().getSubject();
				}
				roles = auth.getAuthorities().stream()
						.map(a -> a.getAuthority())
						.sorted()
						.collect(Collectors.joining(","));
			}

			AuditLog logEntry = AuditLog.builder()
					.userId(userId)
					.username(username)
					.roles(roles)
					.method(request.getMethod())
					.path(request.getRequestURI())
					.query(request.getQueryString())
					.status(response.getStatus())
					.durationMs(Duration.between(start, Instant.now()).toMillis())
					.clientIp(resolveClientIp(request))
					.userAgent(request.getHeader("User-Agent"))
					.build();

			auditLogService.save(logEntry);
		} catch (Exception ex) {
			log.warn("Failed to save audit log", ex);
		}
	}

	private String resolveClientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			int comma = forwarded.indexOf(',');
			return comma >= 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
		}
		return request.getRemoteAddr();
	}
}
