package io.kestra.plugin.hubspot;

import io.kestra.core.models.property.Property;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.net.URI;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractUpdateTask extends HubspotConnection {

    @Schema(
        title = "Additional HubSpot properties",
        description = "Optional key-value map merged into the PATCH body. Property names must match HubSpot field keys."
    )
    protected Property<Map<String, Object>> additionalProperties;

    @Getter
    @Builder
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Updated record ID"
        )
        private Long id;

        @Schema(
            title = "URI of stored properties",
            description = "Internal storage URI containing the updated record properties."
        )
        private URI uri;
    }
}
