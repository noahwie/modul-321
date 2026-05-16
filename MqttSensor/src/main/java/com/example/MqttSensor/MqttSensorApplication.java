package com.example.MqttSensor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

@SpringBootApplication
public class MqttSensorApplication {

	public static void main(String[] args) throws Exception {

		String broker = "tcp://localhost:1883";
		String clientId = "java-sensor-1";
		String publishTopic = "sensoren/java1";
		String subscribeTopic = "feedback/java1";

		MqttClient client = new MqttClient(broker, clientId, new MemoryPersistence());

		MqttConnectOptions options = new MqttConnectOptions();
		options.setCleanSession(true);
		client.connect(options);
		System.out.println("Verbunden mit Broker: " + broker);

		// Stop-Flag
		final boolean[] running = {true};

		// Feedback-Topic abonnieren
		client.subscribe(subscribeTopic, (topic, message) -> {
			String payload = new String(message.getPayload());
			System.out.println("[feedback] Empfangen: " + payload);
			if (payload.trim().equals("stop")) {
				System.out.println("Stop-Befehl empfangen – beende Schleife.");
				running[0] = false;
			}
		});

		// Sinus-Schleife
		double counter = 0.0;
		while (running[0]) {
			double sinValue = Math.sin(counter);
			System.out.printf("[sensoren/java1] counter=%.1f → sin=%.4f%n", counter, sinValue);

			String payload = String.valueOf(sinValue);
			MqttMessage mqttMessage = new MqttMessage(payload.getBytes());
			mqttMessage.setQos(0);
			client.publish(publishTopic, mqttMessage);

			counter += 0.1;
			Thread.sleep(1000);
		}

		client.disconnect();
		client.close();
		System.out.println("Verbindung getrennt.");
	}
}
