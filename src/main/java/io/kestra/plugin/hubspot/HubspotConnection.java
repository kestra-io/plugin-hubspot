package io.kestra.plugin.hubspot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.kestra.core.serializers.JacksonMapper;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Flux;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class HubspotConnection extends Task {

    protected final static ObjectMapper mapper = JacksonMapper.ofJson(false);
    public static final String HUBSPOT_URL = "https://api.hubapi.com";
    public static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    @Schema(title = "HubSpot API key")
    @PluginProperty(dynamic = true, group = "connection")
    private Property<String> apiKey;

    @Schema(title = "HubSpot OAuth token")
    @PluginProperty(dynamic = true, group = "connection")
    private Property<String> oauthToken;

    @Schema(title = "The HTTP client configuration.")
    HttpConfiguration options;

    public <T> T makeCall(RunContext runContext, HttpRequest.HttpRequestBuilder requestBuilder, Class<T> responseType) throws Exception {
        try (HttpClient client = new HttpClient(runContext, options)) {
            HttpRequest request = requestBuilder.build();
            HttpResponse<T> response = client.request(request, responseType);
            return response.getBody();
        } catch (HttpClientResponseException e) {
            throw cleanHubspotException(e);
        }
    }

    /**
     * Parses the raw HubSpot error body and returns a RuntimeException with a
     * clean, human-readable message. The original exception is preserved as the
     * cause so no stack trace information is lost.
     *
     * HubSpot error bodies look like:
     * [0x36]{"status":"error","message":"...","errors":[{"message":"Developer
     * was not one of the allowed options: [label: \"Accounting\"\nvalue:
     * \"accounting\"\n...]","code":"INVALID_OPTION"}]}
     *
     * This strips the hex-length prefix, deserialises the JSON, and collapses
     * the YAML-style option list inside each error message down to a tidy
     * comma-separated list of allowed values.
     */
    private RuntimeException cleanHubspotException(HttpClientResponseException e) {
        String rawBody = e.getMessage();
        if (rawBody == null) {
            return new RuntimeException("HubSpot API error (no response body)", e);
        }

        // Strip the "[0xNN]" chunked-encoding length prefix if present
        int jsonStart = rawBody.indexOf('{');
        if (jsonStart < 0) {
            return new RuntimeException("HubSpot API error: " + rawBody, e);
        }
        String jsonBody = rawBody.substring(jsonStart);

        try {
            HubspotErrorResponse errorResponse = mapper.readValue(jsonBody, HubspotErrorResponse.class);

            List<String> cleanMessages = new ArrayList<>();

            if (errorResponse.getErrors() != null) {
                for (HubspotErrorDetail detail : errorResponse.getErrors()) {
                    String msg = detail.getMessage();
                    if (msg == null) {
                        continue;
                    }
                    // The option list is YAML-style inside a JSON string, e.g.:
                    //   [label: "Accounting"\nvalue: "accounting"\n..., label: ...]
                    // Collapse each entry to just its value, producing:
                    //   ["accounting", "administrative", ...]
                    msg = msg.replaceAll(
                        "label:[^,\\]]*?value:\\s*\"([^\"]+)\"[^,\\]]*?(?=[,\\]])",
                        "\"$1\""
                    );
                    // Clean up any leftover escape noise
                    msg = msg.replace("\\n", " ").replace("\\\"", "\"").trim();
                    cleanMessages.add(msg);
                }
            }

            if (!cleanMessages.isEmpty()) {
                return new RuntimeException("HubSpot API error: " + String.join("; ", cleanMessages), e);
            }

            // Fallback: top-level message when the errors array is absent/empty
            if (errorResponse.getMessage() != null) {
                return new RuntimeException("HubSpot API error: " + errorResponse.getMessage(), e);
            }

        } catch (Exception parseException) {
            // JSON parsing failed — return a clean message with the raw body
            return new RuntimeException("HubSpot API error: " + jsonBody, e);
        }

        return new RuntimeException("HubSpot API error (unknown)", e);
    }

    public void getAuthorizedRequest(
        RunContext runContext,
        HttpRequest.HttpRequestBuilder requestBuilder) throws IllegalVariableEvaluationException {
        var apiKeyRendered = runContext.render(this.apiKey).as(String.class);
        if (apiKeyRendered.isPresent()) {
            requestBuilder.addHeader("Authorization", "Bearer " + apiKeyRendered.get()).build();
            return;
        }
        var authorizationTokenRendered = runContext.render(oauthToken).as(String.class);
        if (authorizationTokenRendered.isPresent()) {
            requestBuilder.addHeader("Authorization", "Bearer " + authorizationTokenRendered.get()).build();
            return;
        }
        throw new IllegalArgumentException("Missing required authentication fields");
    }

    protected String buildHubspotURL() {
        return HUBSPOT_URL + getEndpoint();
    }

    protected URI store(RunContext runContext, List<Map<String, Object>> results) throws IOException {
        File tempFile = runContext.workingDir().createTempFile(".ion").toFile();
        try (var output = new BufferedWriter(new FileWriter(tempFile), FileSerde.BUFFER_SIZE)) {
            Flux<Map<String, Object>> recordFlux = Flux.fromIterable(results);
            FileSerde.writeAll(output, recordFlux).block();
            return runContext.storage().putFile(tempFile);
        }
    }

    protected abstract String getEndpoint();

    // -------------------------------------------------------------------------
    // Error deserialization models
    // -------------------------------------------------------------------------

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class HubspotErrorResponse {
        private String status;
        private String message;
        private List<HubspotErrorDetail> errors;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class HubspotErrorDetail {
        private String message;
        private String code;
    }
}