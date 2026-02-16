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
        title = "Search default text properties",
        description = "Full-text query across default text properties for the target object. See [HubSpot search docs](https://developers.hubspot.com/docs/api/crm/search) for query semantics."
    )
    private Property<String> query;

    @Schema(
        title = "Filter groups for the search query"
    )
    private Property<List<Map<String, Object>>> filterGroups;

    @Schema(
        title = "Specific properties to include in the response",
        description = "Optional list of property names. Leave empty to return all properties."
    )
    private Property<List<String>> properties;

    @Schema(
        title = "Maximum number of results to return",
        description = "Default is 10; maximum allowed by HubSpot is 100."
    )
    @Builder.Default
    private Property<Integer> limit = Property.ofValue(10);

    @Schema(
        title = "Pagination token for fetching the next page of results"
    )
    private Property<String> after;

    @Schema(
        title = "Sort options for the results"
    )
    private Property<List<Map<String, String>>> sorts;

    @Schema(
        title = "Whether to fetch all records by paging through results",
        description = "If true, iterates pages until exhausted. Default is false; set to true to retrieve the full result set."
    )
    @Builder.Default
    private Property<Boolean> fetchAllPages = Property.ofValue(false);

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

        boolean shouldFetchAll = runContext.render(this.fetchAllPages).as(Boolean.class).orElse(false);

        while (shouldFetchAll && nextPageToken != null) {
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
            title = "URI of stored results",
            description = "Internal storage URI containing the aggregated search results."
        )
        private URI uri;
    }
}
