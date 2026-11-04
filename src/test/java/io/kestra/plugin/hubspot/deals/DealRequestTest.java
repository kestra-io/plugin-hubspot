package io.kestra.plugin.hubspot.deals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.kestra.core.serializers.JacksonMapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class DealRequestTest {

    private static final ObjectMapper MAPPER = JacksonMapper.ofJson(false);

    @Test
    void shouldSerializeAssociationsInHubspotV3Shape() throws Exception {
        var request = new DealRequest();
        request.setName("Enterprise Software Deal");
        request.setAssociatedCompanyIds(List.of(123L));
        request.setAssociatedContactIds(List.of(456L));

        var json = MAPPER.readTree(MAPPER.writeValueAsString(request));
        var associations = json.get("associations");

        assertThat(associations.size(), is(2));

        var company = associations.get(0);
        assertThat(company.get("to").get("id").asText(), is("123"));
        assertThat(company.get("to").get("id").isTextual(), is(true));
        assertThat(company.get("types").get(0).get("associationCategory").asText(), is("HUBSPOT_DEFINED"));
        assertThat(company.get("types").get(0).get("associationTypeId").asLong(), is(DealRequest.DEAL_TO_COMPANY));

        var contact = associations.get(1);
        assertThat(contact.get("to").get("id").asText(), is("456"));
        assertThat(contact.get("types").get(0).get("associationTypeId").asLong(), is(DealRequest.DEAL_TO_CONTACT));
    }

    @Test
    void shouldNotSendTheOldFlatShape() throws Exception {
        var request = new DealRequest();
        request.setAssociatedCompanyIds(List.of(123L));

        var association = MAPPER.readTree(MAPPER.writeValueAsString(request)).get("associations").get(0);

        assertThat(association.get("to").isObject(), is(true));
        assertThat(association.has("id"), is(false));
    }

    @Test
    void shouldOmitAssociationsWhenNoneAreGiven() throws Exception {
        var request = new DealRequest();
        request.setName("Deal without associations");

        var body = MAPPER.readValue(MAPPER.writeValueAsString(request), Map.class);

        assertThat(body.containsKey("associations"), is(false));
        assertThat(((Map<?, ?>) body.get("properties")).get("dealname"), is("Deal without associations"));
    }

    @Test
    void shouldBuildTheDefaultAssociationUriForUpdates() {
        assertThat(
            Update.associationUri("987", "companies", 123L).toString(),
            is("https://api.hubapi.com/crm/v4/objects/deals/987/associations/default/companies/123")
        );
        assertThat(
            Update.associationUri("987", "contacts", 456L).toString(),
            is("https://api.hubapi.com/crm/v4/objects/deals/987/associations/default/contacts/456")
        );
    }
}
