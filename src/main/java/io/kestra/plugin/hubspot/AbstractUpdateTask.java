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
        title = "Additional properties for the record"
    )
    protected Property<Map<String, Object>> additionalProperties;

    @Getter
    @Builder
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Created record ID"
        )
        private Long id;

        @Schema(
            title = "URI of the file "
        )
        private URI uri;
    }
}
