package io.kestra.plugin.hubspot.companies;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.hubspot.AbstractCreateTask;
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
    title = "Create a HubSpot company.",
    description =  "Check out the [Hubspot API documentation](https://developers.hubspot.com/docs/reference/api/crm/objects/companies) to learn more."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            code = """
                id: hubspot_companies_create
                namespace: company.team

                tasks:
                  - id: create_company
                    type: io.kestra.plugin.hubspot.companies.Create
                    apiKey: "{{ secret('HUBSPOT_API_KEY') }}"
                    name: "Acme Corporation"
                    domain: "acme.com"
                    companyDescription: "Leading provider of innovative solutions"
                    industry: "ACCOUNTING"
                    companyType: "PARTNER"
                    additionalProperties:
                      city: "San Francisco"
                      state: "CA"
                """
        )
    }
)
public class Create extends AbstractCreateTask implements RunnableTask<AbstractCreateTask.Output> {

    public static final String HUBSPOT_OBJECT_ENDPOINT = "/crm/v3/objects/companies";

    @Schema(
        title = "Company name",
        description = "The name of the company (required)"
    )
    @NotNull
    private Property<String> name;

    @Schema(
        title = "Company domain"
    )
    @NotNull
    private Property<String> domain;

    @Schema(
        title = "Company companyDescription"
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

        CompanyRequest request = new CompanyRequest();

        request.setName(runContext.render(this.name).as(String.class).orElseThrow());

        request.setDomain(runContext.render(this.domain).as(String.class).orElseThrow());

        runContext.render(this.companyDescription).as(String.class).ifPresent(request::setDescription);

        runContext.render(this.industry).as(String.class).ifPresent(request::setIndustry);

        runContext.render(this.companyType).as(String.class).ifPresent(request::setType);

        if (this.additionalProperties != null) {
            Map<String, Object> additionalProps = runContext.render(this.additionalProperties).asMap(String.class, Object.class);
            request.setAdditionalProperties(additionalProps);
        }

        URI uri = URI.create(buildHubspotURL());

        String requestBody = mapper.writeValueAsString(request);

        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
                .uri(uri)
                .addHeader("Content-Type", JSON_CONTENT_TYPE)
                .method("POST")
                .body(HttpRequest.StringRequestBody.builder().content(requestBody).build());

        getAuthorizedRequest(runContext, requestBuilder);

        HubspotResponse response = makeCall(runContext, requestBuilder, HubspotResponse.class);

        URI fileURI = store(runContext, List.of(response.getProperties()));

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