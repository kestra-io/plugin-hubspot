package io.kestra.plugin.hubspot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompanyRequest {

    private Map<String, Object> properties = new HashMap<>();

    @JsonIgnore
    public void setName(String name) {
        if (name != null) {
            properties.put("name", name);
        }
    }

    @JsonIgnore
    public void setDomain(String domain) {
        if (domain != null) {
            properties.put("domain", domain);
        }
    }

    @JsonIgnore
    public void setDescription(String description) {
        if (description != null) {
            properties.put("description", description);
        }
    }

    @JsonIgnore
    public void setIndustry(String industry) {
        if (industry != null) {
            properties.put("industry", industry);
        }
    }

    @JsonIgnore
    public void setType(String type) {
        if (type != null) {
            properties.put("type", type);
        }
    }

    @JsonIgnore
    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        if (additionalProperties != null) {
            properties.putAll(additionalProperties);
        }
    }
}