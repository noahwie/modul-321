package com.example.MqttSensor;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttSensorApplication {

	public static void main(String[] args) throws Exception {

		// Standardwerte
		String publishTopic = "sensoren/java1";
		String subscribeTopic = "feedback/java1";

		// Kommandozeilen-Parameter auslesen (--pub=X, --sub=X)
		for (String arg : args) {
			if (arg.startsWith("--pub=")) {
				publishTopic = "sensoren/java" + arg.substring(6);
			} else if (arg.startsWith("--sub=")) {
				subscribeTopic = "feedback/java" + arg.substring(6);
			}
		}

		// Umgebungsvariable MQTT_BROKER (Fallback: localhost)
		String brokerHost = System.getenv("MQTT_BROKER");
		if (brokerHost == null || brokerHost.isEmpty()) {
			brokerHost = "localhost";
		}
		String broker = "tcp://" + brokerHost + ":1883";

		System.out.println("Broker:    " + broker);
		System.out.println("PUB Topic: " + publishTopic);
		System.out.println("SUB Topic: " + subscribeTopic);

		String clientId = "java-sensor-" + publishTopic.replace("/", "-");
		MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());

		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		client.connect(options);
		System.out.println("Verbunden mit Broker: " + broker);

		final boolean[] running = {true};
		final String finalSubscribeTopic = subscribeTopic;

		// Feedback-Topic abonnieren
		client.subscribe(subscribeTopic, (topic, message) -> {
			String payload = new String(message.getPayload());
			System.out.println("[" + finalSubscribeTopic + "] Empfangen: " + payload);
			if (payload.trim().equals("stop")) {
				System.out.println("Stop-Befehl empfangen – beende Schleife.");
				running[0] = false;
			}
		});

		// Sinus-Schleife
		double counter = 0.0;
		final String finalPublishTopic = publishTopic;
		while (running[0]) {
			double sinValue = Math.sin(counter);
			System.out.printf("[%s] counter=%.1f → sin=%.4f%n", finalPublishTopic, counter, sinValue);

			MqttMessage mqttMessage = new MqttMessage(String.valueOf(sinValue).getBytes());
			mqttMessage.setQos(0);
			client.publish(finalPublishTopic, mqttMessage);

			counter += 0.1;
			Thread.sleep(1000);
		}

		client.disconnect();
		client.close();
		System.out.println("Verbindung getrennt.");
	}
}