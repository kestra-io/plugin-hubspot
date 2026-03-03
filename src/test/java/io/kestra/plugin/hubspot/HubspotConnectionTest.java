package io.kestra.plugin.hubspot;

import java.lang.reflect.Method;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.Test;

import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.junit.annotations.KestraTest;

@KestraTest
class HubspotConnectionTest {

    /**
     * Builds a fake HttpClientResponseException using the same message format
     * that FailedResponseInterceptor produces:
     *   "Failed http request with response code '400' and body:\n<rawJson>"
     */
    private HttpClientResponseException fakeException(String rawJson) throws Exception {
        String message = "Failed http request with response code '400' and body:\n" + rawJson;

        // HttpClientResponseException(String message, HttpResponse<?> response)
        // HttpResponse is a @Value Lombok class — build a minimal one via its builder
        HttpResponse<?> fakeResponse = HttpResponse.builder()
            .status(HttpResponse.Status.builder().code(400).reason("Bad Request").build())
            .request(null)
            .headers(null)
            .body(null)
            .endpointDetail(null)
            .build();

        return new HttpClientResponseException(message, fakeResponse);
    }

    /**
     * Invokes the private cleanHubspotException method via reflection so we can
     * test it without changing its visibility.
     */
    private RuntimeException invokeClean(HttpClientResponseException e) throws Exception {
        // Use a concrete anonymous subclass to instantiate the abstract class
        HubspotConnection instance = new HubspotConnection() {
            @Override
            protected String getEndpoint() {
                return "/test";
            }
        };

        Method method = HubspotConnection.class.getDeclaredMethod(
            "cleanHubspotException", HttpClientResponseException.class
        );
        method.setAccessible(true);
        return (RuntimeException) method.invoke(instance, e);
    }

    @Test
    void shouldRemoveEscapeSequencesFromErrorMessage() throws Exception {
        // This is the exact raw JSON body from issue #35
        String rawJson = "{\"status\":\"error\","
            + "\"message\":\"Property values were not valid\","
            + "\"errors\":[{"
            + "\"message\":\"Developer was not one of the allowed options: "
            + "[label: \\\"Accounting\\\"\\nvalue: \\\"accounting\\\"\\n"
            + "display_order: 0\\nhidden: false\\nread_only: false\\n, "
            + "label: \\\"Administrative\\\"\\nvalue: \\\"administrative\\\"\\n"
            + "display_order: 1\\nhidden: false\\nread_only: false\\n]\","
            + "\"code\":\"INVALID_OPTION\"}]}";

        RuntimeException result = invokeClean(fakeException(rawJson));

        // Should contain a clean, readable message
        assertThat(result.getMessage(), containsString("HubSpot API error:"));
        assertThat(result.getMessage(), containsString("accounting"));
        assertThat(result.getMessage(), containsString("administrative"));

        // Should NOT contain the raw escape noise from the original error
        assertThat(result.getMessage(), not(containsString("\\n")));
        assertThat(result.getMessage(), not(containsString("\\\"Accounting\\\"")));
        assertThat(result.getMessage(), not(containsString("display_order")));
        assertThat(result.getMessage(), not(containsString("hidden: false")));
        assertThat(result.getMessage(), not(containsString("read_only: false")));
    }

    @Test
    void shouldPreserveOriginalExceptionAsCause() throws Exception {
        String rawJson = "{\"status\":\"error\","
            + "\"message\":\"Some error\","
            + "\"errors\":[{\"message\":\"Invalid value\",\"code\":\"INVALID_OPTION\"}]}";

        HttpClientResponseException original = fakeException(rawJson);
        RuntimeException result = invokeClean(original);

        // Original must be preserved as cause so no stack trace is lost
        assertThat(result.getCause(), org.hamcrest.Matchers.is(original));
    }

    @Test
    void shouldFallBackToTopLevelMessageWhenErrorsArrayIsEmpty() throws Exception {
        String rawJson = "{\"status\":\"error\","
            + "\"message\":\"Something went wrong\","
            + "\"errors\":[]}";

        RuntimeException result = invokeClean(fakeException(rawJson));

        assertThat(result.getMessage(), containsString("Something went wrong"));
    }

    @Test
    void shouldHandleNullBodyGracefully() throws Exception {
        HttpResponse<?> fakeResponse = HttpResponse.builder()
            .status(HttpResponse.Status.builder().code(400).reason("Bad Request").build())
            .request(null)
            .headers(null)
            .body(null)
            .endpointDetail(null)
            .build();

        // Message with no JSON body at all
        HttpClientResponseException e = new HttpClientResponseException(
            "Failed http request with response code '400'", fakeResponse
        );

        RuntimeException result = invokeClean(e);

        assertThat(result.getMessage(), containsString("HubSpot API error"));
    }
}