package io.kestra.plugin.hubspot.companies;

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
        title = "Deletes a company from HubSpot."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            code = """
                id: hubspot_companies_delete
                namespace: company.team

                inputs:
                  - id: company_id
                    type: STRING

                tasks:
                  - id: delete_company
                    type: io.kestra.plugin.hubspot.companies.Delete
                    apiKey: "{{ secret('HUBSPOT_API_KEY') }}"
                    companyId: "{{ inputs.company_id }}"
                """
                )
        }
)
public class Delete extends AbstractDeleteTask implements RunnableTask<VoidOutput> {

    public static final String HUBSPOT_OBJECT_ENDPOINT = "/crm/v3/objects/companies";

    @Schema(
            title = "Company ID"
    )
    @NotNull
    private Property<String> companyId;

    @Override
    public VoidOutput run(RunContext runContext) throws Exception {

        String recordId = runContext.render(companyId).as(String.class).orElseThrow();

        super.run(runContext,recordId);

        return null;
    }

    @Override
    protected String getEndpoint() {
        return HUBSPOT_OBJECT_ENDPOINT;
    }
}