package com.smarthome.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class DeviceSimulatorApplication {

	public static void main(String[] args) throws Exception {
		String brokerUrl = getEnv("SIM_BROKER_URL", "ssl://localhost:8883");
		String clientId = getEnv("SIM_CLIENT_ID", "device-" + UUID.randomUUID());
		long telemetryIntervalMs = Long.parseLong(getEnv("SIM_TELEMETRY_INTERVAL_MS", "5000"));
		String topicTelemetry = "devices/" + clientId + "/telemetry";
		String topicCmd = "devices/" + clientId + "/cmd";
		AtomicReference<TempUnit> tempUnit = new AtomicReference<>(TempUnit.C);

		String keystorePath = getEnv("SIM_KEYSTORE_PATH", "");
		String keystorePassword = getEnv("SIM_KEYSTORE_PASSWORD", "");
		String keyPassword = getEnv("SIM_KEY_PASSWORD", keystorePassword);
		String truststorePath = getEnv("SIM_TRUSTSTORE_PATH", "");
		String truststorePassword = getEnv("SIM_TRUSTSTORE_PASSWORD", "");

		MqttConnectOptions options = new MqttConnectOptions();
		options.setServerURIs(new String[]{brokerUrl});
		options.setAutomaticReconnect(true);
		options.setCleanSession(true);
		options.setConnectionTimeout(10);
		options.setKeepAliveInterval(20);

		if (!keystorePath.isBlank() && !truststorePath.isBlank()) {
			SSLContext sslContext = sslContext(keystorePath, keystorePassword, keyPassword, truststorePath, truststorePassword);
			options.setSocketFactory(sslContext.getSocketFactory());
		}

		MqttClient client = new MqttClient(brokerUrl, clientId);
		client.setCallback(new MqttCallback() {
			@Override
			public void connectionLost(Throwable cause) {
				log.warn("Connection lost", cause);
			}
			@Override
			public void messageArrived(String topic, MqttMessage message) {
				String payload = new String(message.getPayload());
				log.info("Command received on {}: {}", topic, payload);
				TempUnit nextUnit = parseUnitCommand(payload);
				if (nextUnit != null && nextUnit != tempUnit.get()) {
					tempUnit.set(nextUnit);
					log.info("Temperature unit changed to {}", nextUnit);
				}
			}
			@Override
			public void deliveryComplete(IMqttDeliveryToken token) { }
		});

		client.connect(options);
		log.info("Device {} connected to {}", clientId, brokerUrl);
		client.subscribe(topicCmd, 1);

		ObjectMapper mapper = new ObjectMapper();
		Random random = new Random();
		while (true) {
			TempUnit currentUnit = tempUnit.get();
			double temperatureC = 20 + random.nextDouble() * 5;
			double temperature = currentUnit == TempUnit.C ? temperatureC : TempUnit.toFahrenheit(temperatureC);
			Map<String, Object> telemetry = new HashMap<>();
			telemetry.put("temperature", temperature);
			telemetry.put("unit", currentUnit.name());
			telemetry.put("humidity", 40 + random.nextDouble() * 10);
			telemetry.put("status", "OK");
			telemetry.put("ts", Instant.now().toString());
			byte[] payload = mapper.writeValueAsBytes(telemetry);
			client.publish(topicTelemetry, new MqttMessage(payload));
			Thread.sleep(telemetryIntervalMs);
		}
	}

	private static TempUnit parseUnitCommand(String payload) {
		String trimmed = payload.trim();
		if (trimmed.equalsIgnoreCase("C")) {
			return TempUnit.C;
		}
		if (trimmed.equalsIgnoreCase("F")) {
			return TempUnit.F;
		}
		try {
			ObjectMapper mapper = new ObjectMapper();
			String unit = null;
			var root = mapper.readTree(trimmed);
			if (root.hasNonNull("unit")) {
				unit = root.get("unit").asText();
			} else if (root.hasNonNull("temperatureUnit")) {
				unit = root.get("temperatureUnit").asText();
			}
			if (unit != null) {
				return TempUnit.from(unit);
			}
		} catch (Exception ex) {
			// ignore malformed command payloads
		}
		return null;
	}

	private enum TempUnit {
		C,
		F;

		static TempUnit from(String value) {
			if (value == null) {
				return null;
			}
			if ("C".equalsIgnoreCase(value)) {
				return C;
			}
			if ("F".equalsIgnoreCase(value)) {
				return F;
			}
			return null;
		}

		static double toFahrenheit(double celsius) {
			return (celsius * 9 / 5) + 32;
		}
	}

	private static SSLContext sslContext(String ksPath, String ksPass, String keyPass, String tsPath, String tsPass) throws Exception {
		KeyStore ks = KeyStore.getInstance("PKCS12");
		try (FileInputStream fis = new FileInputStream(ksPath)) {
			ks.load(fis, ksPass.toCharArray());
		}
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(ks, keyPass.toCharArray());

		KeyStore ts = KeyStore.getInstance("JKS");
		try (FileInputStream fis = new FileInputStream(tsPath)) {
			ts.load(fis, tsPass.toCharArray());
		}
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ts);

		SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
		sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		return sslContext;
	}

	private static String getEnv(String name, String def) {
		String v = System.getenv(name);
		return v != null ? v : def;
	}
}

