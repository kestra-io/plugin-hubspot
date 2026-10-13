package io.kestra.plugin.hubspot.tickets;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.TicketingTaskInterface;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.hubspot.AbstractCreateTask;
import io.kestra.plugin.hubspot.HubspotResponse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import io.kestra.core.models.annotations.PluginProperty;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Create HubSpot ticket record",
    description = "Creates a ticket via HubSpot CRM v3. Supports optional pipeline, stage (default 1), and priority; stores returned properties to internal storage."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            code = """
                id: hubspot_tickets_create
                namespace: company.team

                tasks:
                  - id: create_ticket
                    type: io.kestra.plugin.hubspot.tickets.Create
                    apiKey: "{{ secret('HUBSPOT_API_KEY') }}"
                    subject: "Increased 5xx in Demo Service"
                    content: "The number of 5xx has increased beyond the threshold for Demo service."
                    stage: 3
                    priority: HIGH
                """
        ),
        @Example(
            title = "Create an issue when a Kestra workflow in any namespace with `company` as prefix fails.",
            full = true,
            code = """
                id: create_ticket_on_failure
                namespace: system

                tasks:
                  - id: create_ticket
                    type: io.kestra.plugin.hubspot.tickets.Create
                    apiKey: "{{ secret('HUBSPOT_API_KEY') }}"
                    subject: Workflow failed
                    content: "{{ execution.id }} has failed on {{ taskrun.startDate }}"
                    stage: 3
                    priority: HIGH

                triggers:
                  - id: on_failure
                    type: io.kestra.plugin.core.trigger.Flow
                    conditions:
                      - type: io.kestra.plugin.core.condition.ExecutionStatus
                        in:
                          - FAILED
                          - WARNING
                      - type: io.kestra.plugin.core.condition.ExecutionNamespace
                        namespace: company
                        comparison: PREFIX
                """
        )
    }
)
public class Create extends AbstractCreateTask implements RunnableTask<Create.Output>, TicketingTaskInterface {

    public static final String HUBSPOT_TICKET_ENDPOINT = "/crm/v3/objects/tickets";
    private static final String ACCOUNT_DETAILS_ENDPOINT = "/account-info/v3/details";
    private static final String DEFAULT_UI_DOMAIN = "app.hubspot.com";

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH;
    }

    @Schema(
        title = "Ticket subject",
        description = "Subject line for the ticket. HubSpot requires a subject on creation."
    )
    @PluginProperty(group = "main")
    private Property<String> subject;

    @Schema(
        title = "Ticket body",
        description = "Body/description content of the ticket."
    )
    @PluginProperty(group = "advanced")
    private Property<String> content;

    @Schema(
        title = "Ticket pipeline",
        description = "Optional pipeline ID. If omitted, HubSpot default pipeline applies."
    )
    @PluginProperty(group = "advanced")
    private Property<Integer> pipeline;

    @Schema(
        title = "Ticket pipeline stage",
        description = "Stage ID within the pipeline. Defaults to 1."
    )
    @Builder.Default
    @PluginProperty(group = "advanced")
    private Property<Integer> stage = Property.ofValue(1);

    @Schema(
        title = "Ticket priority",
        description = "Optional priority value: LOW, MEDIUM, or HIGH."
    )
    @PluginProperty(group = "advanced")
    private Property<Priority> priority;

    @Schema(
        title = "HubSpot portal ID",
        description = "Numeric ID of the HubSpot account, used to build the `url` output. When omitted, it is looked up from the account the token belongs to."
    )
    @PluginProperty(group = "connection")
    private Property<String> portalId;

    @Override
    public Create.Output run(RunContext runContext) throws Exception {

        Logger logger = runContext.logger();

        TicketRequest request = new TicketRequest(
            runContext.render(this.subject).as(String.class).orElse(null),
            runContext.render(this.content).as(String.class).orElse(null),
            runContext.render(this.stage).as(Integer.class).orElse(null)
        );

        if (this.priority != null) {
            request.setPriority(runContext.render(this.priority).as(Priority.class).orElseThrow().name());
        }

        if (this.pipeline != null) {
            request.setHsPipeline(runContext.render(this.pipeline).as(Integer.class).orElseThrow());
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

        logger.info("Created HubSpot record: {}", response);

        URI fileURI = store(runContext, List.of(response.getProperties()));

        logger.info("Created HubSpot ticket: {}", response);

        return Output.builder()
            .id(response.getId())
            .uri(fileURI)
            .url(ticketUrl(runContext, logger, response.getId()))
            .build();
    }

    @Override
    protected String getEndpoint() {
        return HUBSPOT_TICKET_ENDPOINT;
    }

    static String buildTicketUrl(String uiDomain, String portalId, Long ticketId) {
        return "https://" + uiDomain + "/contacts/" + portalId + "/record/0-5/" + ticketId;
    }

    private String ticketUrl(RunContext runContext, Logger logger, Long ticketId) throws Exception {
        Optional<String> explicitPortalId = runContext.render(this.portalId).as(String.class);
        if (explicitPortalId.isPresent()) {
            return buildTicketUrl(DEFAULT_UI_DOMAIN, explicitPortalId.get(), ticketId);
        }

        try {
            HttpRequest.HttpRequestBuilder request = HttpRequest.builder()
                .uri(URI.create(HUBSPOT_URL + ACCOUNT_DETAILS_ENDPOINT))
                .method("GET");
            getAuthorizedRequest(runContext, request);
            AccountDetails details = makeCall(runContext, request, AccountDetails.class);
            if (details != null && details.getPortalId() != null) {
                String uiDomain = details.getUiDomain() != null ? details.getUiDomain() : DEFAULT_UI_DOMAIN;
                return buildTicketUrl(uiDomain, String.valueOf(details.getPortalId()), ticketId);
            }
        } catch (Exception e) {
            logger.warn("Could not look up the HubSpot portal ID, so the ticket URL is not set; set `portalId` to provide it", e);
        }
        return null;
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Created record ID"
        )
        private Long id;

        @Schema(
            title = "URI of stored properties",
            description = "Internal storage URI containing the created record properties."
        )
        private URI uri;

        @Schema(
            title = "Ticket URL",
            description = "Link that opens the created ticket in the HubSpot UI. Empty when the portal ID is not set and could not be looked up."
        )
        private String url;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AccountDetails {
        private Long portalId;
        private String uiDomain;
    }
}
