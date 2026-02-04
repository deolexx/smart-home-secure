package com.smarthome.hub.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	private final JwtGrantedAuthoritiesConverter defaultConverter;

	public CustomJwtGrantedAuthoritiesConverter() {
		this.defaultConverter = new JwtGrantedAuthoritiesConverter();
		this.defaultConverter.setAuthorityPrefix("ROLE_");
		this.defaultConverter.setAuthoritiesClaimName("realm_access.roles");
	}

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
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
	}
}
