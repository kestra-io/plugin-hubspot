package io.kestra.plugin.hubspot.deals;

import com.google.common.base.Strings;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.hubspot.AbstractTaskIT;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledIf;

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
        RunContext runContext = runContextFactory.of();

        assertThat(dealId, is(notNullValue()));

        Update task = Update.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .dealId(Property.ofValue(String.valueOf(dealId)))
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
}
