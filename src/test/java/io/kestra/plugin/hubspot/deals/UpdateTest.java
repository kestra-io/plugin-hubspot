package io.kestra.plugin.hubspot.deals;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.hubspot.AbstractUpdateTask;
import io.kestra.plugin.hubspot.HubspotConnection;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KestraTest
class UpdateTest {
    @Inject
    private RunContextFactory runContextFactory;

    private HttpServer server;
    private final AtomicReference<String> requestMethod = new AtomicReference<>();
    private final AtomicReference<String> requestPath = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        // Stands in for the HubSpot API: records the request and answers like a successful PATCH.
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange ->
        {
            requestMethod.set(exchange.getRequestMethod());
            requestPath.set(exchange.getRequestURI().getPath());
            byte[] body = "{\"id\":12345,\"properties\":{\"dealname\":\"Renamed deal\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void shouldRenderDealIdExpressionIntoRequestUrl() throws Exception {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();

        // Same task, with only the base URL pointed at the local server instead of api.hubapi.com.
        Update task = new Update() {
            @Override
            protected String buildHubspotURL() {
                return baseUrl + getEndpoint();
            }
        };
        setField(task, Update.class, "dealId", Property.ofExpression("{{ inputs.deal_id }}"));
        setField(task, Update.class, "name", Property.ofValue("Renamed deal"));
        setField(task, HubspotConnection.class, "apiKey", Property.ofValue("test-key"));

        RunContext runContext = runContextFactory.of(Map.of("inputs", Map.of("deal_id", "12345")));

        AbstractUpdateTask.Output output = task.run(runContext);

        assertThat(requestMethod.get(), is("PATCH"));
        assertThat(requestPath.get(), is(Update.HUBSPOT_DEAL_ENDPOINT + "/12345"));
        assertThat(output.getId(), is(12345L));
        assertThat(output.getUri(), is(notNullValue()));
    }

    private static void setField(Object target, Class<?> owner, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
