package io.kestra.plugin.hubspot.tickets;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

class CreateUrlTest {

    private static Create.AccountDetails account(Long portalId, String uiDomain) {
        Create.AccountDetails details = new Create.AccountDetails();
        details.setPortalId(portalId);
        details.setUiDomain(uiDomain);
        return details;
    }

    @Test
    void shouldUseTheLookedUpPortalAndDomainWhenNoPortalIdIsSet() {
        assertThat(Create.resolveTicketUrl(Optional.empty(), account(123L, "app-eu1.hubspot.com"), 9L), is("https://app-eu1.hubspot.com/contacts/123/record/0-5/9"));
    }

    @Test
    void shouldKeepTheLookedUpDomainWhenAPortalIdIsSetExplicitly() {
        assertThat(Create.resolveTicketUrl(Optional.of("456"), account(123L, "app-eu1.hubspot.com"), 9L), is("https://app-eu1.hubspot.com/contacts/456/record/0-5/9"));
    }

    @Test
    void shouldFallBackToTheDefaultDomainWhenTheLookupFailedButAPortalIdIsSet() {
        assertThat(Create.resolveTicketUrl(Optional.of("456"), null, 9L), is("https://app.hubspot.com/contacts/456/record/0-5/9"));
    }

    @Test
    void shouldReturnNoUrlWhenNeitherThePortalIdNorTheLookupIsAvailable() {
        assertThat(Create.resolveTicketUrl(Optional.empty(), null, 9L), is(nullValue()));
    }
}
