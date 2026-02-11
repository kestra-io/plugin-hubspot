package io.kestra.plugin.hubspot.deals;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.hubspot.AbstractSearchTask;
import io.swagger.v3.oas.annotations.media.Schema;
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
    title = "Search HubSpot deal records",
    description = "Runs HubSpot CRM search on deals with optional query, filters, sorts, and property selection. Default limit is 10; set `fetchAllPages` to true to page through all results."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            code = """
                id: hubspot_deals_search
                namespace: company.team

                tasks:
                  - id: search_deals
                    type: io.kestra.plugin.hubspot.deals.Search
                    apiKey: "{{ secret('HUBSPOT_API_KEY') }}"
                    properties:
                      - name
                    limit: 10
                    sorts:
                      - propertyName: "createdate"
                        direction: "DESCENDING"
                """
        )
    }
)
public class Search extends AbstractSearchTask implements RunnableTask<AbstractSearchTask.Output> {

    public static final String HUBSPOT_OBJECT_ENDPOINT = "/crm/v3/objects/deals";

    @Override
    public Output run(RunContext runContext) throws Exception {
        return super.run(runContext);
    }

    @Override
    protected String getEndpoint() {
        return HUBSPOT_OBJECT_ENDPOINT;
    }
}
