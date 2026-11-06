package io.kestra.plugin.hubspot.deals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledIf;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.serializers.JacksonMapper;
import io.kestra.plugin.hubspot.AbstractTaskIT;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisabledIf(
    value = "isApiKeyNull",
    disabledReason = "For CI/CD as requires secret apiKey or oauthToken"
)
public class DealIT extends AbstractTaskIT {

    @Inject
    private RunContextFactory runContextFactory;

    private static Long dealId;

    @Test
    @Order(1)
    void shouldCreateDealWhenValidInputIsProvided() throws Exception {
        RunContext runContext = runContextFactory.of();

        Create task = Create.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .name(Property.ofValue("Example deal"))
            .stage(Property.ofValue("qualifiedtobuy"))
            .pipeline(Property.ofValue("default"))
            .build();

        Create.Output runOutput = task.run(runContext);

        assertThat(runOutput.getId(), is(notNullValue()));
        dealId = runOutput.getId();
    }

    @Test
    @Order(2)
    void shouldSearchWhenSearchingByName() throws Exception {
        RunContext runContext = runContextFactory.of();

        Search task = Search.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .query(Property.ofValue("Example deal"))
            .build();

        Search.Output runOutput = task.run(runContext);

        assertThat(runOutput.getTotal(), greaterThanOrEqualTo(1));
    }

    @Test
    @Order(3)
    void shouldUpdateCompanyWhenValidIdIsGiven() throws Exception {
        assertThat(dealId, is(notNullValue()));

        RunContext runContext = runContextFactory.of(Map.of("deal_id", String.valueOf(dealId)));

        Update task = Update.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .dealId(Property.ofExpression("{{ deal_id }}"))
            .name(Property.ofValue("updated deal name"))
            .build();

        Update.Output runOutput = task.run(runContext);

        assertThat(runOutput.getId(), is(notNullValue()));
        assertThat(runOutput.getId(), is(dealId));
    }

    @Test
    @Order(4)
    void shouldSearchDealWhenNameIsUpdated() throws Exception {
        RunContext runContext = runContextFactory.of();

        Search task = Search.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .query(Property.ofValue("updated deal name"))
            .build();

        Search.Output runOutput = task.run(runContext);

        assertThat(runOutput.getTotal(), greaterThanOrEqualTo(1));
    }

    @Test
    @Order(5)
    void shouldDeleteDealWhenValidIdIsGiven() throws Exception {
        RunContext runContext = runContextFactory.of();

        assertThat(dealId, is(notNullValue()));

        Delete task = Delete.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .dealId(Property.ofValue(String.valueOf(dealId)))
            .build();

        VoidOutput runOutput = task.run(runContext);
    }

    @Test
    @Order(6)
    void shouldAssociateCompaniesAndContactsOnCreateAndUpdate() throws Exception {
        var runContext = runContextFactory.of();
        var suffix = String.valueOf(System.currentTimeMillis());

        var companyId = io.kestra.plugin.hubspot.companies.Create.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .name(Property.ofValue("Association company " + suffix))
            .domain(Property.ofValue("assoc-" + suffix + ".example.com"))
            .build()
            .run(runContext)
            .getId();
        var contactId = io.kestra.plugin.hubspot.contacts.Create.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .email(Property.ofValue("assoc-" + suffix + "@example.com"))
            .build()
            .run(runContext)
            .getId();

        var created = Create.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .name(Property.ofValue("Association deal " + suffix))
            .stage(Property.ofValue("qualifiedtobuy"))
            .pipeline(Property.ofValue("default"))
            .associatedCompanyIds(Property.ofValue(List.of(companyId)))
            .build()
            .run(runContext);
        var associatedDealId = String.valueOf(created.getId());

        try {
            assertThat(associatedIds(associatedDealId, "companies"), hasItem(String.valueOf(companyId)));

            Update.builder()
                .apiKey(Property.ofValue(getApiKey()))
                .dealId(Property.ofValue(associatedDealId))
                .associatedContactIds(Property.ofValue(List.of(contactId)))
                .build()
                .run(runContext);

            assertThat(associatedIds(associatedDealId, "contacts"), hasItem(String.valueOf(contactId)));
        } finally {
            Delete.builder().apiKey(Property.ofValue(getApiKey())).dealId(Property.ofValue(associatedDealId)).build().run(runContext);
            io.kestra.plugin.hubspot.companies.Delete.builder().apiKey(Property.ofValue(getApiKey())).companyId(Property.ofValue(String.valueOf(companyId))).build().run(runContext);
            io.kestra.plugin.hubspot.contacts.Delete.builder().apiKey(Property.ofValue(getApiKey())).contactId(Property.ofValue(String.valueOf(contactId))).build().run(runContext);
        }
    }

    private static List<String> associatedIds(String dealId, String toObjectType) throws Exception {
        var request = HttpRequest.newBuilder(
                URI.create("https://api.hubapi.com/crm/v4/objects/deals/" + dealId + "/associations/" + toObjectType))
            .header("Authorization", "Bearer " + getApiKey())
            .GET()
            .build();
        try (var client = HttpClient.newHttpClient()) {
            var body = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            var ids = new ArrayList<String>();
            JacksonMapper.ofJson().readTree(body).get("results").forEach(r -> ids.add(r.get("toObjectId").asText()));
            return ids;
        }
    }
}
