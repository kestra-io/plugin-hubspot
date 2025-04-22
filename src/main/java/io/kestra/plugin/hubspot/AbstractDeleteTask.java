package io.kestra.plugin.hubspot;

import io.kestra.core.http.HttpRequest;
import io.kestra.core.models.tasks.VoidOutput;
import io.kestra.core.runners.RunContext;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.net.URI;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractDeleteTask extends HubspotConnection {

    public VoidOutput run(RunContext runContext, String recordId) throws Exception {

        Logger logger = runContext.logger();

        URI uri = URI.create(buildHubspotURL() + "/" + recordId);

        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
            .uri(uri)
            .method("DELETE");

        getAuthorizedRequest(runContext, requestBuilder);

        makeCall(runContext, requestBuilder, VoidOutput.class);

        return null;
    }
}