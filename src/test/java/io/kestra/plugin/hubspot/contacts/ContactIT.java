package io.kestra.plugin.hubspot.contacts;

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
public class ContactIT extends AbstractTaskIT {

    @Inject
    private RunContextFactory runContextFactory;

    private static Long contactId;

    @Test
    @Order(1)
    void shouldCreateContactWhenInputIsValid() throws Exception {
        RunContext runContext = runContextFactory.of();

        Create task = Create.builder()
            .apiKey(Property.of(getApiKey()))
            .email(Property.of(System.currentTimeMillis() + "example@example.com"))
            .firstName(Property.of("example first name"))
            .lastName(Property.of("example last name"))
            .build();

        Create.Output runOutput = task.run(runContext);

        assertThat(runOutput.getId(), is(notNullValue()));
        contactId = runOutput.getId();
    }

    @Test
    @Order(2)
    void shouldFindContactWhenNameIsSearched() throws Exception {
        RunContext runContext = runContextFactory.of();

        Search task = Search.builder()
            .apiKey(Property.of(getApiKey()))
            .query(Property.of("example first name"))
            .build();

        Search.Output runOutput = task.run(runContext);

        assertThat(runOutput.getTotal(), greaterThanOrEqualTo(1));
    }

    @Test
    @Order(3)
    void shouldUpdateContactWhenValidIdIsGiven() throws Exception {
        RunContext runContext = runContextFactory.of();

        assertThat(contactId, is(notNullValue()));

        Update task = Update.builder()
            .apiKey(Property.of(getApiKey()))
            .contactId(Property.of(String.valueOf(contactId)))
            .firstName(Property.of("updated first name"))
            .build();

        Update.Output runOutput = task.run(runContext);

        assertThat(runOutput.getId(), is(notNullValue()));
        assertThat(runOutput.getId(), is(contactId));
    }

    @Test
    @Order(4)
    void shouldFindContactWhenNameIsUpdated() throws Exception {
        RunContext runContext = runContextFactory.of();

        assertThat(contactId, is(notNullValue()));

        Delete task = Delete.builder()
            .apiKey(Property.of(getApiKey()))
            .contactId(Property.of(String.valueOf(contactId)))
            .build();

        VoidOutput runOutput = task.run(runContext);
    }

    @Test
    @Order(5)
    void shouldDeleteContactWhenValidIdIsGiven() throws Exception {
        RunContext runContext = runContextFactory.of();

        Search task = Search.builder()
            .apiKey(Property.of(getApiKey()))
            .query(Property.of("example first name"))
            .build();

        Search.Output runOutput = task.run(runContext);

        assertThat(runOutput.getTotal(), greaterThanOrEqualTo(1));
    }
}
