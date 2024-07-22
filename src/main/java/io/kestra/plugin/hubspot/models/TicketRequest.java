package io.kestra.plugin.hubspot.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TicketRequest {
    private Properties properties;

    public TicketRequest(String subject, String content, Integer hsPipelineStage) {
        this.properties = new Properties(subject, content, hsPipelineStage);
    }

    public void setHsPipeline(Integer hsPipeline) {
        this.properties.setHsPipeline(hsPipeline);
    }

    public void setPriority(String priority) {
        this.properties.setHsTicketPriority(priority);
    }

    @Data
    @RequiredArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Properties {

        private final String subject;

        private final String content;

        @JsonProperty("hs_pipeline_stage")
        private final Integer hsPipelineStage;

        @JsonProperty("hs_pipeline")
        private Integer hsPipeline;

        @JsonProperty("hs_ticket_priority")
        private String hsTicketPriority;

    }
}
