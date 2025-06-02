package io.kestra.plugin.hubspot.tickets;

import com.google.common.base.Strings;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.hubspot.AbstractTaskIT;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KestraTest
@DisabledIf(
    value = "isApiKeyNull",
    disabledReason = "For CI/CD as requires secret apiKet or oauthToken"
)
class CreateTest extends AbstractTaskIT {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of();

        Create task = Create.builder()
            .apiKey(Property.ofValue(getApiKey()))
            .subject(Property.ofValue("This is a test"))
            .content(Property.ofValue("This is a test from kestra unit tests"))
            .stage(Property.ofValue(4))
            .build();

        Create.Output runOutput = task.run(runContext);

        assertThat(runOutput.getId(), is(notNullValue()));
    }
}
