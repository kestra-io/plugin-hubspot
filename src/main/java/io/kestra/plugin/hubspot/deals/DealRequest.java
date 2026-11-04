package io.kestra.plugin.hubspot.deals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DealRequest {

    /** HubSpot-defined association type for deal to company. */
    public static final long DEAL_TO_COMPANY = 5L;
    /** HubSpot-defined association type for deal to contact. */
    public static final long DEAL_TO_CONTACT = 3L;

    private Map<String, Object> properties = new HashMap<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
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
                associations.add(new DealAssociation(companyId, DEAL_TO_COMPANY));
            }
        }
    }

    @JsonIgnore
    public void setAssociatedContactIds(List<Long> contactIds) {
        if (contactIds != null) {
            for (Long contactId : contactIds) {
                associations.add(new DealAssociation(contactId, DEAL_TO_CONTACT));
            }
        }
    }

    @JsonIgnore
    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        if (additionalProperties != null) {
            properties.putAll(additionalProperties);
        }
    }

    /**
     * One entry of the CRM v3 create `associations` input:
     * {"to":{"id":"123"},"types":[{"associationCategory":"HUBSPOT_DEFINED","associationTypeId":5}]}
     */
    @Data
    public static class DealAssociation {
        private final Map<String, String> to;
        private final List<AssociationType> types;

        public DealAssociation(Long id, long associationTypeId) {
            this.to = Map.of("id", String.valueOf(id));
            this.types = List.of(new AssociationType("HUBSPOT_DEFINED", associationTypeId));
        }
    }

    @Data
    public static class AssociationType {
        private final String associationCategory;
        private final long associationTypeId;
    }
}
