package io.kestra.plugin.hubspot;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class HubspotSearchResponse {
    private List<Result> results;
    private Paging paging;
    private Object total;

    @Data
    public static class Result {
        private String id;
        private Map<String, Object> properties;
        private String createdAt;
        private String updatedAt;
        private boolean archived;
    }

    @Data
    public static class Paging {
        private Map<String, Object> next;
        private Map<String, Object> prev;
    }
}