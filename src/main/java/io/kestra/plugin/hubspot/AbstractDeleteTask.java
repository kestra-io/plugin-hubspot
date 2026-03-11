package io.kestra.plugin.hubspot;

import java.net.URI;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractDeleteTask extends HubspotConnection {

    public VoidOutput run(RunContext runContext, String recordId) throws Exception {

        URI uri = URI.create(buildHubspotURL() + "/" + recordId);

        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
            .uri(uri)
            .method("DELETE");

        getAuthorizedRequest(runContext, requestBuilder);

        makeCall(runContext, requestBuilder, VoidOutput.class);

        return null;
    }
}