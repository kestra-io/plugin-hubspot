package io.kestra.plugin.hubspot.deals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class UpdateAssociationErrorsTest {

    @Test
    void shouldReportInputsRejectedInAMultiStatusResponse() {
        // Shape of HubSpot's 207 body when one of the IDs does not exist.
        Map<String, Object> response = Map.of(
            "status", "COMPLETE",
            "results", List.of(Map.of("fromObjectId", 1, "toObjectId", List.of(123))),
            "numErrors", 1,
            "errors", List.of(Map.of(
                "status", "error",
                "category", "VALIDATION_ERROR",
                "message", "One or more toObjectIds are invalid",
                "context", Map.of("toObjectId", List.of("999"))
            ))
        );

        var rejected = Update.rejectedAssociations(response);

        assertThat(rejected, hasSize(1));
        assertThat(rejected.getFirst(), allOf(containsString("One or more toObjectIds are invalid"), containsString("999")));
    }

    @Test
    void shouldReportNothingWhenEveryInputWasAccepted() {
        Map<String, Object> response = Map.of("status", "COMPLETE", "results", List.of());

        assertThat(Update.rejectedAssociations(response), empty());
        assertThat(Update.rejectedAssociations(null), empty());
    }
}
