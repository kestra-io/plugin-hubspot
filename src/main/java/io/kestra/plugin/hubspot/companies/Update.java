package io.kestra.plugin.hubspot.companies;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.hubspot.AbstractUpdateTask;
import io.kestra.plugin.hubspot.HubspotResponse;
import io.kestra.plugin.hubspot.model.CompanyRequest;
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
        title = "Updates a HubSpot company."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            code = """
                id: hubspot_companies_update
                namespace: company.team

                tasks:
                  - id: update_company
                    type: io.kestra.plugin.hubspot.companies.Update
                    apiKey: my_api_key
                    companyId: "{{ inputs.company_id }}"
                    name: "Updated Company Name"
                    industry: "TECHNOLOGY"
                    additionalProperties:
                      city: "New York"
                      state: "NY"
                """
                )
        }
)
public class Update extends AbstractUpdateTask implements RunnableTask<AbstractUpdateTask.Output> {

    public static final String HUBSPOT_OBJECT_ENDPOINT = "/crm/v3/objects/companies";

    @Schema(
            title = "Company ID"
    )
    @NotNull
    private Property<String> companyId;

    @Schema(
            title = "Company name"
    )
    private Property<String> name;

    @Schema(
            title = "Company domain"
    )
    private Property<String> domain;

    @Schema(
            title = "Company description"
    )
    private Property<String> companyDescription;

    @Schema(
            title = "Company industry"
    )
    private Property<String> industry;

    @Schema(
            title = "Company type",
            description = "For example: Customer, Prospect, Partner"
    )
    private Property<String> companyType;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        String companyIdValue = runContext.render(this.companyId).as(String.class).orElseThrow();

        CompanyRequest request = new CompanyRequest();

        runContext.render(this.name).as(String.class).ifPresent(request::setName);

        runContext.render(this.domain).as(String.class).ifPresent(request::setDomain);

        runContext.render(this.companyDescription).as(String.class).ifPresent(request::setDescription);

        runContext.render(this.industry).as(String.class).ifPresent(request::setIndustry);

        runContext.render(this.companyType).as(String.class).ifPresent(request::setType);

        if (this.additionalProperties != null) {
            Map<String, Object> additionalProps = runContext.render(this.additionalProperties).asMap(String.class, Object.class);
            request.setAdditionalProperties(additionalProps);
        }

        URI uri = URI.create(buildHubspotURL() + "/" + companyIdValue);

        String requestBody = mapper.writeValueAsString(request);

        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
                .uri(uri)
                .addHeader("Content-Type", JSON_CONTENT_TYPE)
                .method("PATCH")
                .body(HttpRequest.StringRequestBody.builder().content(requestBody).build());

        getAuthorizedRequest(runContext, requestBuilder);

        HubspotResponse response = makeCall(runContext, requestBuilder, HubspotResponse.class);

        URI fileURI = store(runContext, List.of(response.getProperties()));

        logger.info("Updated HubSpot company: {}", response);

        return Output.builder()
                .id(response.getId())
                .uri(fileURI)
                .build();
    }

    @Override
    protected String getEndpoint() {
        return HUBSPOT_OBJECT_ENDPOINT;
    }
}