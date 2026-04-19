# Kestra Hubspot Plugin

## What

- Provides plugin components under `io.kestra.plugin.hubspot`.
- Includes classes such as `HubspotConnection`, `HubspotSearchResponse`, `HubspotResponse`, `Delete`.

## Why

- What user problem does this solve? Teams need to create, update, search, and delete HubSpot CRM objects from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps HubSpot steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on HubSpot.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `hubspot`

### Key Plugin Classes

- `io.kestra.plugin.hubspot.companies.Create`
- `io.kestra.plugin.hubspot.companies.Delete`
- `io.kestra.plugin.hubspot.companies.Get`
- `io.kestra.plugin.hubspot.companies.Search`
- `io.kestra.plugin.hubspot.companies.Update`
- `io.kestra.plugin.hubspot.contacts.Create`
- `io.kestra.plugin.hubspot.contacts.Delete`
- `io.kestra.plugin.hubspot.contacts.Get`
- `io.kestra.plugin.hubspot.contacts.Search`
- `io.kestra.plugin.hubspot.contacts.Update`
- `io.kestra.plugin.hubspot.deals.Create`
- `io.kestra.plugin.hubspot.deals.Delete`
- `io.kestra.plugin.hubspot.deals.Get`
- `io.kestra.plugin.hubspot.deals.Search`
- `io.kestra.plugin.hubspot.deals.Update`
- `io.kestra.plugin.hubspot.tickets.Create`

### Project Structure

```
plugin-hubspot/
├── src/main/java/io/kestra/plugin/hubspot/tickets/
├── src/test/java/io/kestra/plugin/hubspot/tickets/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
