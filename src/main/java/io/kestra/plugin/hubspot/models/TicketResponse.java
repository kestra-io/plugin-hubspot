package io.kestra.plugin.hubspot.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TicketResponse {
    private Long id;
    private Properties properties;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Properties {
        private String subject;
        private String content;

    }
}
