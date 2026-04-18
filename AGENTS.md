# Kestra Hubspot Plugin

## What

- Provides plugin components under `io.kestra.plugin.hubspot`.
- Includes classes such as `HubspotConnection`, `HubspotSearchResponse`, `HubspotResponse`, `Delete`.

## Why

- This plugin integrates Kestra with Companies.
- It provides tasks that create, update, search, and delete HubSpot companies.

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
