package io.kestra.plugin.hubspot.contacts;

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
    title = "Create HubSpot contact record",
    description = "Creates a contact via HubSpot CRM v3. Requires API key or OAuth token and stores returned properties to internal storage."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            code = """
                id: hubspot_contacts_create
                namespace: company.team

                tasks:
                  - id: create_contact
                    type: io.kestra.plugin.hubspot.contacts.Create
                    apiKey: "{{ secret('HUBSPOT_API_KEY') }}"
                    email: "john.doe@example.com"
                    firstName: "John"
                    lastName: "Doe"
                    phone: "+1234567890"
                    jobTitle: "Software Engineer"
                    lifecycleStage: "lead"
                """
        )
    }
)
public class Create extends AbstractCreateTask implements RunnableTask<AbstractCreateTask.Output> {

    public static final String HUBSPOT_CONTACT_ENDPOINT = "/crm/v3/objects/contacts";

    @Schema(
        title = "Contact email",
        description = "Required contact email address."
    )
    @NotNull
    private Property<String> email;

    @Schema(
        title = "First name"
    )
    private Property<String> firstName;

    @Schema(
        title = "Last name"
    )
    private Property<String> lastName;

    @Schema(
        title = "Phone number"
    )
    private Property<String> phone;

    @Schema(
        title = "Job title"
    )
    private Property<String> jobTitle;

    @Schema(
        title = "Lifecycle stage",
        description = "HubSpot lifecycle stage key such as subscriber, lead, customer, or opportunity."
    )
    private Property<String> lifecycleStage;

    @Schema(
        title = "Additional HubSpot properties",
        description = "Optional key-value map merged into the request body. Property names must match HubSpot field keys."
    )
    private Property<Map<String, Object>> additionalProperties;

    @Override
    public Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        ContactRequest request = new ContactRequest();

        request.setEmail(runContext.render(this.email).as(String.class).orElseThrow());

        runContext.render(this.firstName).as(String.class).ifPresent(request::setFirstName);

        runContext.render(this.lastName).as(String.class).ifPresent(request::setLastName);

        runContext.render(this.phone).as(String.class).ifPresent(request::setPhone);

        runContext.render(this.jobTitle).as(String.class).ifPresent(request::setJobTitle);

        runContext.render(this.lifecycleStage).as(String.class).ifPresent(request::setLifecycleStage);

        if (this.additionalProperties != null) {
            Map<String, Object> additionalProps = runContext.render(this.additionalProperties).asMap(String.class, Object.class);
            request.setAdditionalProperties(additionalProps);
        }

        URI uri = URI.create(buildHubspotURL());

        String requestBody = mapper.writeValueAsString(request);

        logger.info("Request body: {}", requestBody);

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
        return HUBSPOT_CONTACT_ENDPOINT;
    }
}
