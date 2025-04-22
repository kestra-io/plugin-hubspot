package io.kestra.plugin.hubspot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DealRequest {

    private Map<String, Object> properties = new HashMap<>();
    private List<DealAssociation> associations = new ArrayList<>();

    @JsonIgnore
    public void setName(String name) {
        properties.put("dealname", name);
    }

    @JsonIgnore
    public void setPipeline(String pipeline) {
        properties.put("pipeline", pipeline);
    }

    @JsonIgnore
    public void setStage(String stage) {
        properties.put("dealstage", stage);
    }

    @JsonIgnore
    public void setAmount(Double amount) {
        properties.put("amount", amount);
    }

    @JsonIgnore
    public void setCloseDate(String closeDate) {
        properties.put("closedate", closeDate);
    }

    @JsonIgnore
    public void setDealType(String dealType) {
        properties.put("dealtype", dealType);
    }

    @JsonIgnore
    public void setAssociatedCompanyIds(List<Long> companyIds) {
        if (companyIds != null) {
            for (Long companyId : companyIds) {
                associations.add(new DealAssociation("company", companyId));
            }
        }
    }

    @JsonIgnore
    public void setAssociatedContactIds(List<Long> contactIds) {
        if (contactIds != null) {
            for (Long contactId : contactIds) {
                associations.add(new DealAssociation("contact", contactId));
            }
        }
    }

    @JsonIgnore
    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        if (additionalProperties != null) {
            properties.putAll(additionalProperties);
        }
    }

    @Data
    public static class DealAssociation {
        private final String to;
        private final Long id;

        public DealAssociation(String to, Long id) {
            this.to = to;
            this.id = id;
        }
    }
}