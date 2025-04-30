package io.kestra.plugin.hubspot;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.net.URI;
import java.util.*;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractSearchTask extends HubspotConnection {

    @Schema(
        title = "Search all default text properties in records of the specified object. [Learn more](https://developers.hubspot.com/docs/api/crm/search)"
    )
    private Property<String> query;

    @Schema(
        title = "Filter groups for the search query"
    )
    private Property<List<Map<String, Object>>> filterGroups;

    @Schema(
        title = "Specific properties to include in the response",
        description = "Leave empty to get all properties"
    )
    private Property<List<String>> properties;

    @Schema(
        title = "Maximum number of results to return",
        description = "Default is 10, maximum is 100"
    )
    @Builder.Default
    private Property<Integer> limit = Property.of(10);

    @Schema(
        title = "Pagination token for fetching the next page of results"
    )
    private Property<String> after;

    @Schema(
        title = "Sort options for the results"
    )
    private Property<List<Map<String, String>>> sorts;

    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        Map<String, Object> requestBody = new HashMap<>();

        runContext.render(this.query).as(String.class)
            .ifPresent(v -> requestBody.put("query", v));

        Optional.ofNullable(runContext.render(this.filterGroups).asList(Map.class))
            .ifPresent(v -> requestBody.put("filterGroups", v));

        Optional.ofNullable(runContext.render(this.properties).asList(String.class))
            .ifPresent(v -> requestBody.put("properties", v));

        runContext.render(this.limit).as(Integer.class)
            .ifPresent(v -> requestBody.put("limit", v));

        Optional<String> renderedAfter =  runContext.render(this.after).as(String.class);
            renderedAfter.ifPresent(v -> requestBody.put("after", v));

        Optional.ofNullable(runContext.render(this.sorts).asList(Map.class))
            .ifPresent(v -> requestBody.put("sorts", v));

        URI uri = URI.create(buildHubspotURL() + "/search");

        String requestBodyString = mapper.writeValueAsString(requestBody);

        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
            .uri(uri)
            .addHeader("Content-Type", JSON_CONTENT_TYPE)
            .method("POST")
            .body(HttpRequest.StringRequestBody.builder().content(requestBodyString).build());

        getAuthorizedRequest(runContext, requestBuilder);

        logger.info("Sending request to {} with payload {}", uri, requestBodyString);

        HubspotSearchResponse response = makeCall(runContext, requestBuilder, HubspotSearchResponse.class);

        List<Map<String, Object>> allResults = new ArrayList<>();
        for (HubspotSearchResponse.Result result : response.getResults()) {
            allResults.add(result.getProperties());
        }

        Map<String,Object> nextPageToken = response.getPaging() != null ? response.getPaging().getNext() : null;
        while (nextPageToken != null) {
            requestBody.put("after", nextPageToken.get("after"));
            requestBodyString = mapper.writeValueAsString(requestBody);

            requestBuilder = HttpRequest.builder()
                .uri(uri)
                .addHeader("Content-Type", JSON_CONTENT_TYPE)
                .method("POST")
                .body(HttpRequest.StringRequestBody.builder().content(requestBodyString).build());

            getAuthorizedRequest(runContext, requestBuilder);

            logger.info("Next page request body: {}", requestBodyString);

            response = makeCall(runContext, requestBuilder, HubspotSearchResponse.class);

            for (HubspotSearchResponse.Result result : response.getResults()) {
                allResults.add(result.getProperties());
            }

            nextPageToken = response.getPaging() != null ? response.getPaging().getNext() : null;
        }

        URI fileURI = store(runContext, allResults);

        logger.info("Retrieved {} records", allResults.size());

        return Output.builder()
            .total(allResults.size())
            .uri(fileURI)
            .build();
    }

    @Getter
    @Builder
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Total number of records fetched"
        )
        private Integer total;

        @Schema(
            title = "URI of the file "
        )
        private URI uri;
    }
}