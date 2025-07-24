package io.kestra.plugin.hubspot.deals;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.hubspot.AbstractGetTask;
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
    title = "Gets a HubSpot deal by ID."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            code = """
                id: hubspot_deals_get
                namespace: company.team

                inputs:
                  - id: deal_id
                    type: STRING

                tasks:
                  - id: get_deals
                    type: io.kestra.plugin.hubspot.deals.Get
                    apiKey: "{{ secret('HUBSPOT_API_KEY') }}"
                    dealId: "{{ inputs.deal_id }}"
                    properties:
                      - name
                """
        )
    }
)
public class Get extends AbstractGetTask implements RunnableTask<AbstractGetTask.Output> {

    public static final String HUBSPOT_OBJECT_ENDPOINT = "/crm/v3/objects/deals";

    @Schema(
        title = "Deal ID"
    )
    @NotNull
    private Property<String> dealId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        return super.run(runContext, runContext.render(dealId).as(String.class).orElseThrow());
    }

    @Override
    protected String getEndpoint() {
        return HUBSPOT_OBJECT_ENDPOINT;
    }
}