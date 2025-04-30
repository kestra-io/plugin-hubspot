package io.kestra.plugin.hubspot.contacts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactRequest {

    private Map<String, Object> properties = new HashMap<>();

    @JsonIgnore
    public void setEmail(String email) {
        properties.put("email", email);
    }

    @JsonIgnore
    public void setFirstName(String firstName) {
        properties.put("firstname", firstName);
    }

    @JsonIgnore
    public void setLastName(String lastName) {
        properties.put("lastname", lastName);
    }

    @JsonIgnore
    public void setPhone(String phone) {
        properties.put("phone", phone);
    }

    @JsonIgnore
    public void setJobTitle(String jobTitle) {
        properties.put("jobtitle", jobTitle);
    }

    @JsonIgnore
    public void setLifecycleStage(String lifecycleStage) {
        properties.put("lifecyclestage", lifecycleStage);
    }

    @JsonIgnore
    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        if (additionalProperties != null) {
            properties.putAll(additionalProperties);
        }
    }
}