package io.kestra.plugin.hubspot.contacts;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.hubspot.AbstractDeleteTask;
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
        title = "Delete HubSpot contact by ID",
        description = "Deletes a contact record via HubSpot CRM v3. Operation is irreversible; requires API key or OAuth token."
)
@Plugin(
        examples = {
                @Example(
                full = true,
                code = """
                id: hubspot_contacts_delete
                namespace: contact.team

                inputs:
                  - id: contact_id
                    type: STRING

                tasks:
                  - id: delete_contact
                    type: io.kestra.plugin.hubspot.contacts.Delete
                    apiKey: "{{ secret('HUBSPOT_API_KEY') }}"
                    contactId: "{{ inputs.contact_id }}"
                """
                )
        }
)
public class Delete extends AbstractDeleteTask implements RunnableTask<VoidOutput> {

    public static final String HUBSPOT_OBJECT_ENDPOINT = "/crm/v3/objects/contacts";

    @Schema(
            title = "Contact ID",
            description = "Required HubSpot contact record ID."
    )
    @NotNull
    private Property<String> contactId;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {

        String recordId = runContext.render(contactId).as(String.class).orElseThrow();

        super.run(runContext,recordId);

        return null;
    }

    @Override
    protected String getEndpoint() {
        return HUBSPOT_OBJECT_ENDPOINT;
    }
}
