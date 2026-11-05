package io.kestra.plugin.hubspot.deals;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.hubspot.AbstractUpdateTask;
import io.kestra.plugin.hubspot.HubspotResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Update HubSpot deal properties",
    description = "PATCH an existing deal via HubSpot CRM v3. Requires record ID and updates only provided fields; stores returned properties to internal storage."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            code = """
                id: hubspot_deals_update
                namespace: company.team

                inputs:
                  - id: deal_id
                    type: STRING

                tasks:
                  - id: update_deal
                    type: io.kestra.plugin.hubspot.deals.Update
                    apiKey: "{{ secret('HUBSPOT_API_KEY') }}"
                    dealId: "{{ inputs.deal_id }}"
                    name: "Enterprise Software Deal"
                    amount: 50000
                    closeDate: "2024-12-31"
                    additionalProperties:
                      description: Important Enterprise Deal to close
                """
        )
    }
)
public class Update extends AbstractUpdateTask implements RunnableTask<AbstractUpdateTask.Output> {

    public static final String HUBSPOT_DEAL_ENDPOINT = "/crm/v3/objects/deals";

    @Schema(
        title = "Deal ID",
        description = "Required HubSpot deal record ID to update."
    )
    @NotNull
    @PluginProperty(group = "main")
    private Property<String> dealId;

    @Schema(
        title = "Deal name",
        description = "New deal name; optional."
    )
    @PluginProperty(group = "destination")
    private Property<String> name;

    @Schema(
        title = "Pipeline ID",
        description = "Pipeline identifier for the deal; optional on update."
    )
    @PluginProperty(group = "advanced")
    private Property<String> pipeline;

    @Schema(
        title = "Deal stage",
        description = "Stage key within the selected pipeline; optional."
    )
    @PluginProperty(group = "advanced")
    private Property<String> stage;

    @Schema(
        title = "Deal amount",
        description = "Optional amount value."
    )
    @PluginProperty(group = "advanced")
    private Property<Double> amount;

    @Schema(
        title = "Close date",
        description = "Optional close date string (e.g., ISO 8601)."
    )
    @PluginProperty(group = "advanced")
    private Property<String> closeDate;

    @Schema(
        title = "Deal type",
        description = "Optional HubSpot deal type (e.g., newbusiness or existingbusiness)."
    )
    @PluginProperty(group = "advanced")
    private Property<String> dealType;

    @Schema(
        title = "Associated company IDs",
        description = """
            Optional list of HubSpot company record IDs to associate.
            Each one is linked with the unlabeled `Deal to company` association (type 341), so none of them becomes the primary company; set the primary company in HubSpot if you need one.
            """
    )
    @PluginProperty(group = "advanced")
    private Property<List<Long>> associatedCompanyIds;

    @Schema(
        title = "Associated contact IDs",
        description = "Optional list of HubSpot contact record IDs to associate."
    )
    @PluginProperty(group = "advanced")
    private Property<List<Long>> associatedContactIds;

    @Schema(
        title = "Additional HubSpot properties",
        description = "Optional key-value map merged into the PATCH body. Property names must match HubSpot field keys."
    )
    @PluginProperty(group = "advanced")
    private Property<Map<String, Object>> additionalProperties;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        String rDealId = runContext.render(this.dealId).as(String.class).orElseThrow();

        DealRequest request = new DealRequest();

        runContext.render(this.name).as(String.class).ifPresent(request::setName);

        runContext.render(this.stage).as(String.class).ifPresent(request::setStage);

        runContext.render(this.pipeline).as(String.class).ifPresent(request::setPipeline);

        runContext.render(this.amount).as(Double.class).ifPresent(request::setAmount);

        runContext.render(this.closeDate).as(String.class).ifPresent(request::setCloseDate);

        runContext.render(this.dealType).as(String.class).ifPresent(request::setDealType);

        // PATCH only accepts properties; associations are created afterwards by associate().
        List<Long> companyIds = this.associatedCompanyIds != null
            ? DealRequest.ids(runContext.render(this.associatedCompanyIds).asList(Object.class))
            : List.of();
        List<Long> contactIds = this.associatedContactIds != null
            ? DealRequest.ids(runContext.render(this.associatedContactIds).asList(Object.class))
            : List.of();

        if (this.additionalProperties != null) {
            Map<String, Object> additionalProps = runContext.render(this.additionalProperties).asMap(String.class, Object.class);
            request.setAdditionalProperties(additionalProps);
        }

        URI uri = URI.create(buildHubspotURL() + "/" + rDealId);

        String requestBody = mapper.writeValueAsString(request);

        logger.info("Request body: {}", requestBody);

        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
            .uri(uri)
            .addHeader("Content-Type", JSON_CONTENT_TYPE)
            .method("PATCH")
            .body(HttpRequest.StringRequestBody.builder().content(requestBody).build());

        getAuthorizedRequest(runContext, requestBuilder);

        HubspotResponse response = makeCall(runContext, requestBuilder, HubspotResponse.class);

        logger.info("Updated HubSpot record: {}", response);

        associate(runContext, rDealId, "companies", companyIds);
        associate(runContext, rDealId, "contacts", contactIds);

        URI fileURI = store(runContext, List.of(response.getProperties()));

        return Output.builder()
            .id(response.getId())
            .uri(fileURI)
            .build();
    }

    // One batch call per object type: https://developers.hubspot.com/docs/api-reference/crm-associations-v4/guide
    private void associate(RunContext runContext, String dealId, String toObjectType, List<Long> toObjectIds) throws Exception {
        if (toObjectIds.isEmpty()) {
            return;
        }

        var requestBuilder = HttpRequest.builder()
            .uri(associationUri(toObjectType))
            .addHeader("Content-Type", JSON_CONTENT_TYPE)
            .method("POST")
            .body(HttpRequest.StringRequestBody.builder().content(mapper.writeValueAsString(associationBody(dealId, toObjectIds))).build());

        getAuthorizedRequest(runContext, requestBuilder);

        try {
            makeCall(runContext, requestBuilder, Map.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Deal " + dealId + " was updated, but associating it with " + toObjectType + " " + toObjectIds + " failed: " + e.getMessage(), e
            );
        }
        runContext.logger().info("Associated deal {} with {} {}", dealId, toObjectType, toObjectIds);
    }

    static URI associationUri(String toObjectType) {
        return URI.create(HUBSPOT_URL + "/crm/v4/associations/deals/" + toObjectType + "/batch/associate/default");
    }

    static Map<String, Object> associationBody(String dealId, List<Long> toObjectIds) {
        var inputs = toObjectIds.stream()
            .map(id -> Map.of("from", Map.of("id", dealId), "to", Map.of("id", String.valueOf(id))))
            .toList();
        return Map.of("inputs", inputs);
    }

    @Override
    protected String getEndpoint() {
        return HUBSPOT_DEAL_ENDPOINT;
    }
}
