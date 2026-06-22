package com.stationery.auth.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
// The AuditLogger.java class is a Spring Service component used to log user actions across the system
// (such as  USER_REGISTRATION  and  USER_LOGIN ).
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);
    private final HttpClient httpClient = HttpClient.newHttpClient();
    // Injects the base URL of the  request-service  from application properties. If not configured, 
    // it defaults to  http://localhost:8083 .
    @Value("${services.request-service.url:http://localhost:8083}")
    private String requestServiceUrl;

    public void log(String action, String performedBy, String userRole, String details) {
        try {
            // Build json payload safely, escaping quotes in details if necessary
            String escapedDetails = details.replace("\"", "\\\"");
            String jsonPayload = String.format(
                "{\"action\":\"%s\",\"performedBy\":\"%s\",\"userRole\":\"%s\",\"details\":\"%s\",\"createdTime\":\"%s\",\"updatedTime\":\"%s\"}",
                action, performedBy, userRole, escapedDetails, LocalDateTime.now(), LocalDateTime.now()
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(requestServiceUrl + "/api/requests/audit-logs"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            // Send asynchronously to avoid blocking the main transaction
            httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response -> {
                        if (response.statusCode() != 201) {
                            log.warn("Failed to send audit log to request-service. Status: {}", response.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        log.warn("Error sending audit log: {}", ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            log.error("Failed to trigger audit log", e);
        }
    }
}
