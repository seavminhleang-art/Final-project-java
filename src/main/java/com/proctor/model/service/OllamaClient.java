package com.proctor.model.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proctor.exception.AIException;
import com.proctor.config.Config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class OllamaClient {
    private final String baseUrl;
    private final String model;
    private final int timeoutSeconds;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaClient() {
        this.baseUrl = Config.get("ollama.url", "http://localhost:11434");
        this.model = Config.get("ollama.model", "llama3.2:3b");
        this.timeoutSeconds = Config.getInt("ollama.timeout_seconds", 120);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public OllamaClient(String baseUrl, String model, int timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String generateJson(String prompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            requestBody.put("format", "json");

            Map<String, Object> options = new HashMap<>();
            double temp = 0.3;
            String tempStr = Config.get("ollama.temperature", "0.3");
            try {
                temp = Double.parseDouble(tempStr.trim());
            } catch (Exception ignored) {
            }
            options.put("temperature", temp);
            options.put("num_predict", 4096);
            requestBody.put("options", options);

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new AIException("Ollama returned status code " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("response")) {
                return root.get("response").asText();
            } else {
                throw new AIException("Unexpected response from Ollama: " + response.body());
            }
        } catch (Exception e) {
            if (e instanceof AIException ai) throw ai;
            Throwable rootCause = e;
            while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
                rootCause = rootCause.getCause();
            }
            if (e instanceof java.net.ConnectException || rootCause instanceof java.net.ConnectException) {
                throw new AIException("Cannot connect to Ollama at " + baseUrl + ". Please ensure Ollama is running ('ollama serve').", e);
            }
            if (e instanceof java.net.http.HttpConnectTimeoutException || e instanceof java.net.http.HttpTimeoutException
                    || rootCause instanceof java.net.SocketTimeoutException) {
                throw new AIException("Ollama request timed out after " + timeoutSeconds + "s. Model '" + model + "' may be loading into memory.", e);
            }
            String msg = e.getMessage() != null ? e.getMessage() : (rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.getClass().getSimpleName());
            if (msg.contains("Connection refused") || msg.contains("Failed to connect") || msg.contains("ConnectException")) {
                throw new AIException("Cannot connect to Ollama at " + baseUrl + ". Please ensure Ollama is running ('ollama serve').", e);
            }
            if (msg.contains("timed out") || msg.contains("Timeout")) {
                throw new AIException("Ollama request timed out after " + timeoutSeconds + "s. Model '" + model + "' may be loading into memory.", e);
            }
            throw new AIException("Failed to communicate with Ollama at " + baseUrl + ": " + msg, e);
        }
    }
}