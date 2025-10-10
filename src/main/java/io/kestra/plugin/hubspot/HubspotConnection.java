package io.kestra.plugin.hubspot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class HubspotConnection extends Task {

    protected final static ObjectMapper mapper = JacksonMapper.ofJson(false);

    public static final String HUBSPOT_URL = "https://api.hubapi.com";

    public static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    @Schema(
        title = "HubSpot API key"
    )
    @PluginProperty(dynamic = true)
    private Property<String> apiKey;

    @Schema(
        title = "HubSpot OAuth token"
    )
    @PluginProperty(dynamic = true)
    private Property<String> oauthToken;

    @Schema(title = "The HTTP client configuration.")
    HttpConfiguration options;

    public <T> T makeCall(RunContext runContext, HttpRequest.HttpRequestBuilder requestBuilder, Class<T> responseType) throws Exception {Logger logger = runContext.logger();

        try (HttpClient client = new HttpClient(runContext,options)) {

            HttpRequest request = requestBuilder.build();

            HttpResponse<T> response = client.request(request, responseType);

            return response.getBody();
        }
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

    protected URI store(RunContext runContext, List<Map<String, Object>> results) throws IOException, IOException {

        File tempFile = runContext.workingDir().createTempFile(".ion").toFile();

        try (var output = new BufferedWriter(new FileWriter(tempFile), FileSerde.BUFFER_SIZE)) {
            Flux<Map<String, Object>> recordFlux = Flux.fromIterable(results);
            FileSerde.writeAll(output, recordFlux).block();
            return runContext.storage().putFile(tempFile);
        }
    }

    protected abstract String getEndpoint();
}