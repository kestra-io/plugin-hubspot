package io.kestra.plugin.hubspot.contacts;

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
        title = "Gets a HubSpot company by ID."
)
@Plugin(
        examples = {
                @Example(
                        full = true,
                        code = """
                id: hubspot_contacts_get
                namespace: company.team

                inputs:
                  - id: contact_id
                    type: STRING

                tasks:
                  - id: get_company
                    type: io.kestra.plugin.hubspot.contacts.Get
                    apiKey: my_api_key
                    companyId: "{{ inputs.contact_id }}"
                    properties:
                      - email
                """
                )
        }
)
public class Get extends AbstractGetTask implements RunnableTask<AbstractGetTask.Output> {

    public static final String HUBSPOT_OBJECT_ENDPOINT = "/crm/v3/objects/contacts";

    @Schema(
            title = "Contact ID"
    )
    @NotNull
    private Property<String> contactId;

    @Override
    public Output run(RunContext runContext) throws Exception {
        return super.run(runContext, runContext.render(contactId).as(String.class).orElseThrow());
    }

    @Override
    protected String getEndpoint() {
        return HUBSPOT_OBJECT_ENDPOINT;
    }
}