package io.kestra.plugin.hubspot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HubspotResponse {
    private Long id;
    private Map<String,Object> properties;
}