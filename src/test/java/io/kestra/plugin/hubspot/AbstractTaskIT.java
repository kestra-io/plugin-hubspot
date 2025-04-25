package io.kestra.plugin.hubspot;

import com.google.common.base.Strings;
import io.kestra.core.junit.annotations.KestraTest;

@KestraTest
public abstract class AbstractTaskIT {

    private static final String API_KEY = System.getenv("HUBSPOT_API_KEY");

    private static boolean isApiKeyNull() {
        return Strings.isNullOrEmpty(getApiKey());
    }

    protected static String getApiKey() {
        return API_KEY;
    }
}
