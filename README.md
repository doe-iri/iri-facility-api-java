# IRI Facility API — Java Reference Implementation

The **IRI Facility API** is a Java/Spring Boot reference implementation of the DOE Integrated Research Infrastructure (IRI) Facility Status interface. It exposes read-only endpoints for facility metadata, resources, incidents, events, sites, and locations and ships with example data and OpenAPI docs.

---

## Contents
- [Overview](#overview)
- [Quick start](#quick-start)
- [API documentation](#api-documentation)
- [Configuration](#configuration)
- [Endpoints](#endpoints)
- [Examples](#examples)
- [Development](#development)
- [Container usage](#container-usage)
- [Support](#support)
- [License](#license)

---

## Overview

- **Runtime:** Java 21, Spring Boot (parent 3.5.x)  
- **Build:** Maven (wrapper included)  
- **Docs:** OpenAPI + Swagger UI enabled  
- **Data source:** JSON files configurable via `iri.*` settings (defaults provided)

---

## Quick start

### Prerequisites
- Java **21+**
- Maven **3.8+** (or use `./mvnw`)

### Build & run (dev)
```bash
git clone https://github.com/doe-iri/iri-facility-api-java.git
cd iri-facility-api-java
./mvnw clean install
./mvnw spring-boot:run
````

By default the app starts on **[http://localhost:8081](http://localhost:8081)**.

---

## API documentation

* **Swagger UI:** `http://localhost:8081/v3/swagger-ui`
* **OpenAPI JSON:** `http://localhost:8081/v3/api-docs`

> These paths are controlled by `springdoc` settings in `application.yaml`.

---

## Configuration

Application configuration is YAML-based. The repository includes:

* `src/main/resources/application.yaml` (dev defaults)
* `compose/config/application.yaml` (TLS example for containerized run)

Key settings:

```yaml
server:
  port: 8081          # dev default (compose example uses 8443 + TLS)
  servlet:
    context-path: /
  compression:
    enabled: true

springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /v3/swagger-ui

management:
  endpoints:
    web:
      exposure:
        include: health,info

iri:
  server:
    root: "http://localhost:8081"     # or 8443 in the compose example
    # proxy: "https://iri.es.net"
  status:
    facility: src/test/resources/facility.json
    sites: src/test/resources/sites.json
    locations: src/test/resources/locations.json
    incidents: src/test/resources/incidents.json
    events: src/test/resources/events.json
    resources: src/test/resources/resources.json
  account:
    capabilities: src/test/resources/capabilities.json
    projects: src/test/resources/projects.json
    project_allocations: src/test/resources/project_allocations.json
    user_allocations: src/test/resources/user_allocations.json
```

> Swap any of the `iri.status.*` or `iri.account.*` file paths to point at your own JSON sources.

---

## Endpoints

Collection endpoints exposed by the reference server:

| Method | Path                         | Description             |
| -----: | ---------------------------- | ----------------------- |
|    GET | `/api/v1/facility`           | Facility details        |
|    GET | `/api/v1/facility/sites`     | Facility sites          |
|    GET | `/api/v1/facility/locations` | Facility locations      |
|    GET | `/api/v1/status/resources`   | Resource collection     |
|    GET | `/api/v1/status/incidents`   | Incident collection     |
|    GET | `/api/v1/status/events`      | Event collection        |
|    GET | `/v3/api-docs`               | OpenAPI document (JSON) |

> Individual items are discoverable via the `self_uri` fields returned by the collections (e.g., `/api/v1/status/resources/{id}`).

---

## Examples

**List resources**

```bash
curl -s http://localhost:8081/api/v1/status/resources | jq .
```

**Use conditional fetch**

```bash
curl -s \
  -H 'If-Modified-Since: Mon, 01 Jan 2024 00:00:00 GMT' \
  http://localhost:8081/api/v1/status/incidents
```

**Sample resource (truncated)**

```json
{
  "id": "29ea05ad-86de-4df8-b208-f0691aafbaa2",
  "name": "Scratch",
  "description": "The Perlmutter Scratch File System is an all-flash file system.",
  "last_modified": "2025-08-14T05:24:30.000Z",
  "self_uri": "/api/v1/status/resources/29ea05ad-86de-4df8-b208-f0691aafbaa2",
  "member_of_uri": "/api/v1/facility"
}
```

---

## Development

Run the test suite:

```bash
./mvnw test
```

Jar artifact after build:

```
target/iri-facility-api-java-1.0.0.jar
```

---

## Container usage

A multi-stage Dockerfile is provided.

**Build image**

```bash
docker build -t iri-facility-api-java .
```

**Run (HTTP, 8081)**

```bash
docker run --rm -p 8081:8081 iri-facility-api-java
```

**Run with the provided TLS example (8443)**
Mount the example config (includes `application.yaml` with `ssl.enabled: true` and a demo keystore) and publish 8443:

```bash
docker run --rm \
  -p 8443:8443 \
  -v "$(pwd)/compose/config:/iri/config" \
  iri-facility-api-java
```

Environment overrides recognized by the image:

* `CONFIG` — path/URL to an external Spring config (defaults to `file:/iri/config/application.yaml`)
* `LOGBACK` — path/URL to a Logback XML (defaults to `file:/iri/config/logback.xml`)
* `SSL_OPTS`, `DEBUG_OPTS` — passed through to the JVM invocation

---

## Support

Questions and contributions are welcome.

* **Email:** [software@es.net](mailto:software@es.net)
* **Program context:** IRI Interfaces Technical Subcommittee — see the IRI site.

---

## License

Please see the repository’s `LICENSE` file for terms. (If you need an SPDX identifier, review the file—this repository includes an LBNL notice; no SPDX identifier is asserted here.)
