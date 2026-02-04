package com.smarthome.hub.config;

import com.smarthome.hub.service.KeycloakAdminService;
import com.smarthome.hub.service.KeycloakTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties({
		KeycloakAdminService.KeycloakAdminProperties.class,
		KeycloakTokenService.KeycloakClientProperties.class
})
public class KeycloakAdminConfig {

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
