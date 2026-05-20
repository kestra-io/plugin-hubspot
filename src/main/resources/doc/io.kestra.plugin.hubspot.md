# How to use the HubSpot plugin

Manage contacts, companies, deals, and tickets in HubSpot from Kestra flows.

## Authentication

Set `apiKey` for API key auth or `oauthToken` for OAuth token auth — they are mutually exclusive, with `apiKey` taking precedence. Store secrets in [secrets](https://kestra.io/docs/concepts/secret) and apply connection properties globally with [plugin defaults](https://kestra.io/docs/workflow-components/plugin-defaults).

## Tasks

**Contacts** — `contacts.Create` creates a contact with `email` (required) plus optional `firstName`, `lastName`, `phone`, `jobTitle`, `lifecycleStage`, and `additionalProperties`. `contacts.Get` retrieves a contact by `contactId`. `contacts.Search` queries contacts by `query`, `filterGroups`, or `sorts` — set `fetchAllPages: true` to retrieve all pages. `contacts.Update` updates fields on a contact by `contactId`. `contacts.Delete` removes a contact by `contactId`.

**Companies** — `companies.Create` creates a company with `name` and `domain` (both required). `companies.Get`, `companies.Search`, `companies.Update`, and `companies.Delete` follow the same pattern using `companyId`.

**Deals** — `deals.Create` creates a deal with `name`, `pipeline`, and `stage` (all required), plus optional `amount`, `closeDate`, `dealType`, and contact/company association IDs. `deals.Get`, `deals.Search`, `deals.Update`, and `deals.Delete` use `dealId`.

**Tickets** — `tickets.Create` creates a support ticket with `subject` (required) and optional `content`, `pipeline`, `stage`, and `priority` (`LOW`, `MEDIUM`, or `HIGH`).
