package io.kestra.plugin.hubspot;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HubspotResponse {
    private Long id;
    private Map<String, Object> properties;
}