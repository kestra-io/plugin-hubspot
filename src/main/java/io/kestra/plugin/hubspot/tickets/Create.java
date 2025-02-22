package io.kestra.plugin.hubspot.tickets;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.hubspot.HubspotConnection;
import io.kestra.plugin.hubspot.models.TicketRequest;
import io.kestra.plugin.hubspot.models.TicketResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Creates ticket in Hubspot."
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
                    apiKey: my_api_key
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
                    apiKey: my_api_key
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
public class Create extends HubspotConnection implements RunnableTask<Create.Output> {

    public static final String HUBSPOT_TICKET_ENDPOINT = "/crm/v3/objects/tickets";

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH
    }

    @Schema(
        title = "Ticket subject"
    )
    private Property<String> subject;

    @Schema(
        title = "Ticket body"
    )
    private Property<String> content;

    @Schema(
        title = "Ticket pipeline"
    )
    private Property<Integer> pipeline;

    @Schema(
        title = "Ticket pipeline stage"
    )
    @Builder.Default
    private Property<Integer> stage = Property.of(1);

    @Schema(
        title = "Ticket priority",
        description = """
                      (Optional) Available values:
                      LOW: Low priority
                      MEDIUM: Medium priority
                      HIGH: High priority
                      """
    )
    private Property<Priority> priority;

    @Override
    public Create.Output run(RunContext runContext) throws Exception {
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

        String requestBody = mapper.writeValueAsString(request);

        TicketResponse response = makeCall(runContext, requestBody, TicketResponse.class);

        return Output.builder()
            .id(response.getId())
            .build();
    }

    @Override
    protected String getEndpoint() {
        return HUBSPOT_TICKET_ENDPOINT;
    }

    @Getter
    @Builder
    public static class Output implements io.kestra.core.models.tasks.Output {

        @Schema(
            title = "Ticket id"
        )
        private Long id;

    }

}
