package io.kestra.plugin.hubspot.tickets;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
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
    title = "Creates ticket in Hubspot"
)
@Plugin(
    examples = {
        @Example(
            code = """
                   - id: hubspot
                     type: io.kestra.plugin.hubspot.tickets.Create
                     apiKey: my_api_key
                     subject: Workflow failed
                     content: "{{ execution.id }} has failed on {{ taskrun.startDate }}"
                     stage: 3
                     priority: HIGH
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
    @PluginProperty(dynamic = true)
    private String subject;

    @Schema(
        title = "Ticket body"
    )
    @PluginProperty(dynamic = true)
    private String content;

    @Schema(
        title = "Ticket pipeline"
    )
    @PluginProperty
    private Integer pipeline;

    @Schema(
        title = "Ticket pipeline stage"
    )
    @Builder.Default
    @PluginProperty
    private Integer stage = 1;

    @Schema(
        title = "Ticket priority",
        description = """
                      (Optional) Available values:
                      LOW: Low priority
                      MEDIUM: Medium priority
                      HIGH: High priority
                      """
    )
    @PluginProperty
    private Priority priority;

    @Override
    public Create.Output run(RunContext runContext) throws Exception {
        TicketRequest request = new TicketRequest(runContext.render(this.content), runContext.render(this.content), this.stage);

        if (this.priority != null) {
            request.setPriority(this.priority.name());
        }

        if (this.pipeline != null) {
            request.setHsPipeline(this.pipeline);
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
