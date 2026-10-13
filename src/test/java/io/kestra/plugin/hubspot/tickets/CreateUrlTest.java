package io.kestra.plugin.hubspot.tickets;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class CreateUrlTest {

    @Test
    void shouldBuildTheTicketRecordUrlOnTheAccountUiDomain() {
        assertThat(Create.buildTicketUrl("app.hubspot.com", "12345678", 987L), is("https://app.hubspot.com/contacts/12345678/record/0-5/987"));
        assertThat(Create.buildTicketUrl("app-eu1.hubspot.com", "12345678", 987L), is("https://app-eu1.hubspot.com/contacts/12345678/record/0-5/987"));
    }
}
