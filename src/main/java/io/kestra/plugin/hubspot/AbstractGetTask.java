package io.kestra.plugin.hubspot;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.net.URI;
import java.util.List;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractGetTask extends HubspotConnection {

    @Schema(
        title = "Company ID"
    )
    @NotNull
    private Property<String> companyId;

    @Schema(
        title = "Specific properties to include in the response",
        description = "Leave empty to get all properties"
    )
    private Property<List<String>> properties;

    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        String companyIdValue = runContext.render(this.companyId).as(String.class).orElseThrow();

        StringBuilder uriBuilder = new StringBuilder(buildHubspotURL() + "/" + companyIdValue);

        List<String> renderedProperties = runContext.render(properties).asList(String.class);

        if (renderedProperties != null && !renderedProperties.isEmpty()) {
            uriBuilder.append("?properties=").append(String.join(",", renderedProperties));
        }

        URI uri = URI.create(uriBuilder.toString());

        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
            .uri(uri)
            .method("GET");

        getAuthorizedRequest(runContext, requestBuilder);

        HubspotResponse response = makeCall(runContext, requestBuilder, HubspotResponse.class);

        URI fileURI = store(runContext, List.of(response.getProperties()));

        return Output.builder()
            .id(response.getId())
            .uri(fileURI)
            .build();
    }

    @Getter
    @Builder
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Record ID"
        )
        private Long id;

        @Schema(
            title = "URI of the file "
        )
        private URI uri;
    }
}