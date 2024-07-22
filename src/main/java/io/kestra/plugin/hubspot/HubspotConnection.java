package io.kestra.plugin.hubspot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class HubspotConnection extends Task {

    protected final static ObjectMapper mapper = JacksonMapper.ofJson();

    public static final String HUBSPOT_URL = "https://api.hubapi.com";

    public static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    @Schema(
        title = "Hubspot API key"
    )
    @PluginProperty(dynamic = true)
    private String apiKey;

    @Schema(
        title = "Hubspot OAuth token"
    )
    @PluginProperty(dynamic = true)
    private String oauthToken;

    public <T> T makeCall(RunContext runContext, String body, Class<T> clazz) throws Exception {
        String authorizationToken = apiKey != null ? runContext.render(this.apiKey) : runContext.render(this.oauthToken);

        if (Strings.isNullOrEmpty(authorizationToken)) {
            throw new IllegalArgumentException("Either apiKey or oauthToken should be provided");
        }

        try (HttpClient client = HttpClient.newHttpClient()) {

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildHubspotURL()))
                .header("Authorization", "Bearer " + authorizationToken)
                .header("Content-Type", JSON_CONTENT_TYPE)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 201) {
                throw new IOException("Failed to create ticket. Response: " + response.body());
            }

            return mapper.readValue(response.body(), clazz);
        }
    }

    private String buildHubspotURL() {
        return HUBSPOT_URL + getEndpoint();
    }

    protected abstract String getEndpoint();

}
