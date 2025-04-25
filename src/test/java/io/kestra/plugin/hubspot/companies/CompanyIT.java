package io.kestra.plugin.hubspot.companies;

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
public class CompanyIT extends AbstractTaskIT {

    @Inject
    private RunContextFactory runContextFactory;

    private static Long companyId;

    @Test
    @Order(1)
    void shouldCreateCompanyWhenInputIsValid() throws Exception {
        RunContext runContext = runContextFactory.of();

        Create task = Create.builder()
            .apiKey(Property.of(getApiKey()))
            .name(Property.of("Example company"))
            .domain(Property.of("example.co"))
            .build();

        Create.Output runOutput = task.run(runContext);

        assertThat(runOutput.getId(), is(notNullValue()));
        companyId = runOutput.getId();
    }

    @Test
    @Order(2)
    void shouldFindCompanyWhenNameIsSearched() throws Exception {
        RunContext runContext = runContextFactory.of();

        Search task = Search.builder()
            .apiKey(Property.of(getApiKey()))
            .query(Property.of("example.co"))
            .build();

        Search.Output runOutput = task.run(runContext);

        assertThat(runOutput.getTotal(), greaterThanOrEqualTo(1));
    }

    @Test
    @Order(3)
    void shouldUpdateCompanyWhenValidIdIsGiven() throws Exception {
        RunContext runContext = runContextFactory.of();

        assertThat(companyId, is(notNullValue()));

        Update task = Update.builder()
            .apiKey(Property.of(getApiKey()))
            .companyId(Property.of(String.valueOf(companyId)))
            .name(Property.of("Example new company"))
            .domain(Property.of("new.example.co"))
            .build();

        Update.Output runOutput = task.run(runContext);

        assertThat(runOutput.getId(), is(notNullValue()));
        assertThat(runOutput.getId(), is(companyId));
    }

    @Test
    @Order(4)
    void shouldFindCompanyWhenNameIsUpdated() throws Exception {
        RunContext runContext = runContextFactory.of();

        Search task = Search.builder()
            .apiKey(Property.of(getApiKey()))
            .query(Property.of("Example new company"))
            .build();

        Search.Output runOutput = task.run(runContext);

        assertThat(runOutput.getTotal(), greaterThanOrEqualTo(1));
    }

    @Test
    @Order(5)
    void shouldDeleteCompanyWhenValidIdIsGiven() throws Exception {
        RunContext runContext = runContextFactory.of();

        assertThat(companyId, is(notNullValue()));

        Delete task = Delete.builder()
            .apiKey(Property.of(getApiKey()))
            .companyId(Property.of(String.valueOf(companyId)))
            .build();

        VoidOutput runOutput = task.run(runContext);
    }
}
