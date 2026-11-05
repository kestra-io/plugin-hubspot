package io.kestra.plugin.hubspot.deals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KestraTest
class DealIdsTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldAcceptIdsThatRenderAsIntegers() throws Exception {
        var runContext = runContextFactory.of();
        // YAML `- 123` arrives as an Integer, which used to fail with a ClassCastException.
        Property<List<Long>> ids = (Property) Property.ofValue(List.of(123, 124));

        var rendered = DealRequest.ids(runContext.render(ids).asList(Object.class));

        assertThat(rendered, is(List.of(123L, 124L)));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldAcceptIdsWrittenAsStrings() throws Exception {
        var runContext = runContextFactory.of();
        // Quoted IDs, or IDs taken from another task's output, arrive as strings.
        Property<List<Long>> ids = (Property) Property.ofValue(List.of("123", "9876543210"));

        var rendered = DealRequest.ids(runContext.render(ids).asList(Object.class));

        assertThat(rendered, is(List.of(123L, 9876543210L)));
    }

    @Test
    void shouldBuildAssociationsFromIntegerIds() throws Exception {
        var request = new DealRequest();
        request.setAssociatedCompanyIds(DealRequest.ids(List.of(123, 124)));

        assertThat(request.getAssociations().size(), is(2));
    }
}
