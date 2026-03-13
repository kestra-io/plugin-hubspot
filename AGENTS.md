# Kestra Hubspot Plugin

## What

Plugin HubSpot for Kestra Exposes 16 plugin components (tasks, triggers, and/or conditions).

## Why

Enables Kestra workflows to interact with HubSpot, allowing orchestration of HubSpot-based operations as part of data pipelines and automation workflows.

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

### Important Commands

```bash
# Build the plugin
./gradlew shadowJar

# Run tests
./gradlew test

# Build without tests
./gradlew shadowJar -x test
```

### Configuration

All tasks and triggers accept standard Kestra plugin properties. Credentials should use
`{{ secret('SECRET_NAME') }}` — never hardcode real values.

## Agents

**IMPORTANT:** This is a Kestra plugin repository (prefixed by `plugin-`, `storage-`, or `secret-`). You **MUST** delegate all coding tasks to the `kestra-plugin-developer` agent. Do NOT implement code changes directly — always use this agent.
