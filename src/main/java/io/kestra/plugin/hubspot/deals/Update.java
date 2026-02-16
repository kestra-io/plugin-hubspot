package io.kestra.plugin.hubspot.deals;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
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
import org.slf4j.Logger;

import java.net.URI;
import java.util.List;
import java.util.Map;

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
                id: hubspot_deals_create
                namespace: company.team

                inputs:
                  - id: deal_id
                    type: STRING

                tasks:
                  - id: create_deal
                    type: io.kestra.plugin.hubspot.deals.Update
                    apiKey: "{{ secret('HUBSPOT_API_KEY') }}"
                    dealId: {{ inputs.deal_id }}
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
    private Property<String> dealId;

    @Schema(
        title = "Deal name",
        description = "New deal name; optional."
    )
    private Property<String> name;

    @Schema(
        title = "Pipeline ID",
        description = "Pipeline identifier for the deal; optional on update."
    )
    private Property<String> pipeline;

    @Schema(
        title = "Deal stage",
        description = "Stage key within the selected pipeline; optional."
    )
    private Property<String> stage;

    @Schema(
        title = "Deal amount",
        description = "Optional amount value."
    )
    private Property<Double> amount;

    @Schema(
        title = "Close date",
        description = "Optional close date string (e.g., ISO 8601)."
    )
    private Property<String> closeDate;

    @Schema(
        title = "Deal type",
        description = "Optional HubSpot deal type (e.g., newbusiness or existingbusiness)."
    )
    private Property<String> dealType;

    @Schema(
        title = "Associated company IDs",
        description = "Optional list of HubSpot company record IDs to associate."
    )
    private Property<List<Long>> associatedCompanyIds;

    @Schema(
        title = "Associated contact IDs",
        description = "Optional list of HubSpot contact record IDs to associate."
    )
    private Property<List<Long>> associatedContactIds;

    @Schema(
        title = "Additional HubSpot properties",
        description = "Optional key-value map merged into the PATCH body. Property names must match HubSpot field keys."
    )
    private Property<Map<String, Object>> additionalProperties;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        DealRequest request = new DealRequest();

        runContext.render(this.name).as(String.class).ifPresent(request::setName);

        runContext.render(this.stage).as(String.class).ifPresent(request::setStage);

        runContext.render(this.pipeline).as(String.class).ifPresent(request::setPipeline);

        runContext.render(this.amount).as(Double.class).ifPresent(request::setAmount);

        runContext.render(this.closeDate).as(String.class).ifPresent(request::setCloseDate);

        runContext.render(this.dealType).as(String.class).ifPresent(request::setDealType);

        if (this.associatedCompanyIds != null) {
            List<Long> companyIds = runContext.render(this.associatedCompanyIds).asList(Long.class);
            request.setAssociatedCompanyIds(companyIds);
        }

        if (this.associatedContactIds != null) {
            List<Long> contactIds = runContext.render(this.associatedContactIds).asList(Long.class);
            request.setAssociatedContactIds(contactIds);
        }

        if (this.additionalProperties != null) {
            Map<String, Object> additionalProps = runContext.render(this.additionalProperties).asMap(String.class, Object.class);
            request.setAdditionalProperties(additionalProps);
        }


        URI uri = URI.create(buildHubspotURL() + "/" + dealId);

        String requestBody = mapper.writeValueAsString(request);

        logger.info("Request body: {}", requestBody);

        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
            .uri(uri)
            .addHeader("Content-Type", JSON_CONTENT_TYPE)
            .method("PATCH")
            .body(HttpRequest.StringRequestBody.builder().content(requestBody).build());

        getAuthorizedRequest(runContext, requestBuilder);

        HubspotResponse response = makeCall(runContext, requestBuilder, HubspotResponse.class);

        logger.info("Created HubSpot record: {}", response);

        URI fileURI = store(runContext, List.of(response.getProperties()));

        return Output.builder()
            .id(response.getId())
            .uri(fileURI)
            .build();
    }

    @Override
    protected String getEndpoint() {
        return HUBSPOT_DEAL_ENDPOINT;
    }
}
