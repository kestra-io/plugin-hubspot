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
    title = "Create a HubSpot deal."
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
                    type: io.kestra.plugin.hubspot.deals.Create
                    apiKey: my_api_key
                    dealId: {{ inputs.deal_id }}
                    name: "Enterprise Software Deal"
                    amount: 50000
                    closeDate: "2024-12-31"
                    additionalProperties:
                      probability: 0.75
                      notes: "Large enterprise opportunity"
                """
        )
    }
)
public class Update extends AbstractUpdateTask implements RunnableTask<AbstractUpdateTask.Output> {

    public static final String HUBSPOT_DEAL_ENDPOINT = "/crm/v3/objects/deals";

    @Schema(
        title = "Deal ID"
    )
    @NotNull
    private Property<String> dealId;

    @Schema(
        title = "Deal name"
    )
    private Property<String> name;

    @Schema(
        title = "Pipeline ID"
    )
    private Property<String> pipeline;

    @Schema(
        title = "Deal stage"
    )
    private Property<String> stage;

    @Schema(
        title = "Deal amount"
    )
    private Property<Double> amount;

    @Schema(
        title = "Close date"
    )
    private Property<String> closeDate;

    @Schema(
        title = "Deal type"
    )
    private Property<String> dealType;

    @Schema(
        title = "Associated company IDs"
    )
    private Property<List<Long>> associatedCompanyIds;

    @Schema(
        title = "Associated contact IDs"
    )
    private Property<List<Long>> associatedContactIds;

    @Schema(
        title = "Additional properties"
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