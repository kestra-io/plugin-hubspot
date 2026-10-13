package io.kestra.plugin.hubspot.tickets;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.kestra.core.docs.JsonSchemaGenerator;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.tasks.Task;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KestraTest
class CreateSeverityTest {
    @Inject
    JsonSchemaGenerator jsonSchemaGenerator;

    @Test
    @SuppressWarnings("unchecked")
    void shouldMapEveryCaseSeverityToAHubSpotPriority() {
        var inputs = (Map<String, Map<String, Object>>) jsonSchemaGenerator.properties(Task.class, Create.class).get("properties");
        var valueMap = (Map<String, String>) inputs.get("priority").get("$ticketingValueMap");

        assertThat(valueMap, is(Map.of("CRITICAL", "HIGH", "HIGH", "HIGH", "MEDIUM", "MEDIUM", "LOW", "LOW")));
        valueMap.values().forEach(Create.Priority::valueOf);
    }
}
