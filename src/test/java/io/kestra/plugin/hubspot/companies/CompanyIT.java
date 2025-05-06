package io.kestra.plugin.hubspot.companies;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.hubspot.AbstractCreateTask;
import io.kestra.plugin.hubspot.AbstractTaskIT;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
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

        Create.Output runOutput = createCompany("Example company", "example.co");

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

    @Test
    @Order(6)
    void shouldTestPaginationLogicInSearchTask() throws Exception {
        RunContext runContext = runContextFactory.of();

        Create.Output runOutput1 = createCompany("Example company 1", "example.co");
        assertThat(runOutput1.getId(), is(notNullValue()));

        Create.Output runOutput2 = createCompany("Example company 2", "example2.co");
        assertThat(runOutput2.getId(), is(notNullValue()));

        Search task = Search.builder()
            .apiKey(Property.of(getApiKey()))
            .query(Property.of("example.co"))
            .limit(Property.of(1))
            .build();

        Search.Output output = task.run(runContext);

        assertThat(output.getTotal(), greaterThan(0));

        // should get only 1 record, since fetchAllPages is disabled and limit is 1
        assertThat(output.getTotal(), equalTo(1));

        deleteCompanies(runOutput1, runOutput2);
    }

    @Test
    @Order(7)
    void shouldTestPaginationLogicInSearchTaskWhenFetchAllPages() throws Exception {
        RunContext runContext = runContextFactory.of();

        Create.Output runOutput1 = createCompany("Example company 1", "example.co");
        assertThat(runOutput1.getId(), is(notNullValue()));

        Create.Output runOutput2 = createCompany("Example company 2", "example2.co");
        assertThat(runOutput2.getId(), is(notNullValue()));

        Search task = Search.builder()
            .apiKey(Property.of(getApiKey()))
            .query(Property.of("example.co"))
            .limit(Property.of(10))
            .fetchAllPages(Property.of(true))
            .build();

        Search.Output output = task.run(runContext);
        assertThat(output.getTotal(), greaterThanOrEqualTo(10)); // should get more than 1 record, since fetchAllPages is enabled.

        deleteCompanies(runOutput1, runOutput2);
    }

    private Create.Output createCompany(String name, String domain) throws Exception {
        RunContext ctx = runContextFactory.of();
        Create task = Create.builder()
            .apiKey(Property.of(getApiKey()))
            .name(Property.of(name))
            .domain(Property.of(domain))
            .build();
        return task.run(ctx);
    }

    private void deleteCompany(Long id) throws Exception {
        RunContext ctx = runContextFactory.of();
        Delete task = Delete.builder()
            .apiKey(Property.of(getApiKey()))
            .companyId(Property.of(String.valueOf(id)))
            .build();
        task.run(ctx);
    }

    private void deleteCompanies(AbstractCreateTask.Output... outputs) throws Exception {
        for (AbstractCreateTask.Output output : outputs) {
            deleteCompany(output.getId());
        }
    }
}
