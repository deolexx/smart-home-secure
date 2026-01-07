package com.smarthome.hub.mqtt;

import com.smarthome.hub.service.DeviceService;
import com.smarthome.hub.service.TelemetryService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MqttGateway implements MqttCallback {

	private final MqttConfig.MqttProperties props;
	private final MqttConnectOptions options;
	private final DeviceService deviceService;
	private final TelemetryService telemetryService;

	private MqttClient client;

	public MqttGateway(MqttConfig.MqttProperties props,
	                   MqttConnectOptions options,
	                   DeviceService deviceService,
	                   TelemetryService telemetryService) {
		this.props = props;
		this.options = options;
		this.deviceService = deviceService;
		this.telemetryService = telemetryService;
	}

	@PostConstruct
	public void start() {
		try {
			client = new MqttClient(props.getBrokerUrl(), props.getClientId());
			client.setCallback(this);
			client.connect(options);
			log.info("Connected to MQTT broker {}", props.getBrokerUrl());
			client.subscribe(props.getTelemetryTopicFilter(), 1);
			log.info("Subscribed to telemetry topic filter {}", props.getTelemetryTopicFilter());
		} catch (Exception e) {
			log.error("Failed to start MQTT gateway", e);
		}
	}

	public void sendCommand(String deviceClientId, String commandJson) {
		String topic = String.format(props.getCommandTopicFormat(), deviceClientId);
		try {
			client.publish(topic, new MqttMessage(commandJson.getBytes()));
			log.info("Published command to {} payload {}", topic, commandJson);
		} catch (MqttException e) {
			log.error("Failed to publish command", e);
		}
	}

	@Override
	public void connectionLost(Throwable cause) {
		log.warn("MQTT connection lost", cause);
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {
		String payload = new String(message.getPayload());
		log.debug("MQTT message on topic {} payload {}", topic, payload);
		try {
			String[] parts = topic.split("/");
			// Expected pattern: devices/{clientId}/telemetry
			if (parts.length >= 3) {
				String clientId = parts[1];
				deviceService.markOnline(clientId);
				telemetryService.ingestTelemetry(clientId, payload);
			}
		} catch (Exception ex) {
			log.error("Failed to process telemetry message", ex);
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// no-op
	}
}

