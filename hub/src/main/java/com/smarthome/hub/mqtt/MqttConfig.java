package com.smarthome.hub.mqtt;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

@Configuration
public class MqttConfig {

	@Bean
	@ConfigurationProperties(prefix = "mqtt")
	public MqttProperties mqttProperties() {
		return new MqttProperties();
	}

	@Bean
	public MqttConnectOptions mqttConnectOptions(MqttProperties props) throws Exception {
		MqttConnectOptions options = new MqttConnectOptions();
		options.setServerURIs(new String[]{props.getBrokerUrl()});
		options.setAutomaticReconnect(true);
		options.setCleanSession(true);
		options.setConnectionTimeout(10);
		options.setKeepAliveInterval(20);

		if (props.isTlsEnabled()) {
			SSLContext sslContext = sslContext(props);
			options.setSocketFactory(sslContext.getSocketFactory());
		}
		return options;
	}

	private SSLContext sslContext(MqttProperties props) throws Exception {
		KeyManagerFactory kmf = null;
		if (props.getKeystorePath() != null && !props.getKeystorePath().isBlank()) {
			KeyStore ks = KeyStore.getInstance(props.getKeystoreType());
			try (FileInputStream fis = new FileInputStream(props.getKeystorePath())) {
				ks.load(fis, props.getKeystorePassword().toCharArray());
			}
			kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, props.getKeyPassword().toCharArray());
		}

		TrustManagerFactory tmf = null;
		if (props.getTruststorePath() != null && !props.getTruststorePath().isBlank()) {
			KeyStore ts = KeyStore.getInstance(props.getTruststoreType());
			try (FileInputStream fis = new FileInputStream(props.getTruststorePath())) {
				ts.load(fis, props.getTruststorePassword().toCharArray());
			}
			tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(ts);
		}

		// If no keystore or truststore provided, use default SSL context (for development)
		if (kmf == null && tmf == null) {
			return SSLContext.getDefault();
		}

		SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
		sslContext.init(kmf != null ? kmf.getKeyManagers() : null, tmf != null ? tmf.getTrustManagers() : null, null);
		return sslContext;
	}

	@Getter
	@Setter
	public static class MqttProperties {
		private String brokerUrl = "tcp://localhost:1883";
		private boolean tlsEnabled = false;
		private String clientId = "hub-gateway";
		private String telemetryTopicFilter = "devices/+/telemetry";
		private String commandTopicFormat = "devices/%s/cmd";

		// TLS settings
		private String keystorePath;
		private String keystorePassword;
		private String keystoreType = "PKCS12";
		private String keyPassword;
		private String truststorePath;
		private String truststorePassword;
		private String truststoreType = "JKS";
	}
}

