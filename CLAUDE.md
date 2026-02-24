# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**phoss-SMP** is a complete SMP (Service Metadata Publisher) server implementing the Peppol SMP 1.x, OASIS BDXR SMP 1.0, and OASIS BDXR SMP 2.0 specifications. It provides a management GUI and supports three pluggable storage backends: XML (file-based), SQL (MySQL/PostgreSQL/Oracle/DB2), and MongoDB.

Current version: `8.1.0`. License: MPL 2.0 + Apache 2.0.

## Build Commands

```bash
# Full build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Build a specific module
mvn clean install -pl phoss-smp-webapp-sql

# Run tests (requires database - see below)
mvn test -pl phoss-smp-webapp-sql

# Run a single test class
mvn test -Dtest=ServiceGroupInterfaceTest -pl phoss-smp-webapp-sql

# Run a single test method
mvn test -Dtest=ServiceGroupInterfaceTest#testSpecificMethod -pl phoss-smp-webapp-sql
```

**Requirement:** Java 17+ (v8.0+). Maven 3.6+.

### Test Database Setup

Integration tests require live database services. Start them with Docker before running tests:

```bash
docker compose -f unittest-db-docker-compose.yml up
```

This starts MySQL on port 3306 and MongoDB on port 27017. Test configuration lives in `src/test/resources/test-smp-server-sql.properties` (and equivalent for other backends).

### Local Development with Jetty

Run the SMP directly from the IDE without deploying a WAR:

- XML backend: run `RunInJettySMPSERVER_XML` (in `phoss-smp-webapp-xml`)
- SQL backend: run `RunInJettySMPSERVER_SQL` (in `phoss-smp-webapp-sql`)

Default local URL: `http://localhost:90`. Default credentials: `admin@helger.com` / `password`.

For local configuration overrides, place settings in `private-application.properties` alongside `application.properties` — this file is gitignored and takes precedence, so you avoid modifying the committed defaults.

## Module Architecture

The project follows a layered architecture with a backend strategy pattern:

```
phoss-smp-parent-pom (root)
├── phoss-smp-backend          # Core: interfaces, domain model, REST API logic, config
├── phoss-smp-backend-sql      # SQL persistence (JDBC + Flyway migrations)
├── phoss-smp-backend-mongodb  # MongoDB persistence
├── phoss-smp-backend-xml      # XML file-based persistence
├── phoss-smp-webapp           # Core web layer: servlets, UI pages, REST filters
├── phoss-smp-webapp-sql       # Deployable WAR = webapp + backend-sql
├── phoss-smp-webapp-mongodb   # Deployable WAR = webapp + backend-mongodb
└── phoss-smp-webapp-xml       # Deployable WAR = webapp + backend-xml
```

Only the `phoss-smp-webapp-*` modules produce deployable WAR files. The others are libraries.

If dependent Helger libraries (e.g. `ph-oton`) are at SNAPSHOT versions, build them locally first before building this project.

## Backend Plugin System

Backends are registered via Java `ServiceLoader` SPI. Each backend module contains:
- An `ISMPBackendRegistrarSPI` implementation that registers itself with a string ID (`"sql"`, `"mongodb"`, `"xml"`)
- An `ISMPManagerProvider` implementation that vends all manager instances for that backend

The active backend is selected via `smp.backend` in `application.properties`. All backend-specific manager classes implement interfaces defined in `phoss-smp-backend`.

To implement a **custom backend**, follow the pattern of `phoss-smp-backend-xml`:
1. Create a backend sub-project implementing `ISMPManagerProvider` and `ISMPBackendRegistrarSPI`
2. Create a webapp sub-project that references your backend

## Key Domain Concepts

- **ServiceGroup**: A participant (identified by participant ID). The top-level entity.
- **ServiceMetadata / ServiceInformation**: Per-document-type routing info for a ServiceGroup.
- **Redirect**: Pointer to another SMP for a specific document type.
- **BusinessCard**: Extended participant info for the Peppol Directory.
- **TransportProfile**: Supported transport protocols (e.g., `peppol-transport-as4-v2_0`).

Manager interfaces for all entities are in `phoss-smp-backend` under `com.helger.phoss.smp.domain.*`. Implementations live in the respective backend modules.

## REST API

Three API variants are implemented in `phoss-smp-backend/src/main/java/com/helger/phoss/smp/restapi/`:
- `SMPServerAPI` — Peppol SMP 1.x
- `BDXR1ServerAPI` — OASIS BDXR SMP 1.0
- `BDXR2ServerAPI` — OASIS BDXR SMP 2.0

`SMPRestFilter` (in `phoss-smp-webapp`) is mapped to `/*` and dispatches incoming requests to the appropriate API class.

### Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/{ServiceGroupId}` | No | List document types |
| PUT | `/{ServiceGroupId}` | Yes | Create service group |
| DELETE | `/{ServiceGroupId}` | Yes | Delete service group |
| GET | `/{ServiceGroupId}/services/{DocumentTypeId}` | No | Get endpoints |
| PUT | `/{ServiceGroupId}/services/{DocumentTypeId}` | Yes | Define AP endpoints |
| DELETE | `/{ServiceGroupId}/services/{DocumentTypeId}` | Yes | Delete metadata |
| GET | `/businesscard/{ServiceGroupId}` | No | Get business card |
| PUT | `/businesscard/{ServiceGroupId}` | Yes | Create/update business card |
| DELETE | `/businesscard/{ServiceGroupId}` | Yes | Delete business card |
| GET | `/smp-status/` | No | Health/status JSON (disabled by default) |

### Authentication

- **Bearer Token** (preferred, v6.0.7+): `Authorization: Bearer <token>` — tokens created in Administration > Security > User Tokens
- **Basic Auth** (legacy): `Authorization: Basic Base64(email:password)`

Note: the Remote Query API (`/smpquery/*`) is **disabled by default** for security; enable via `smp.rest.remote.queryapi.disabled=false`.

## Web Layer

The management UI uses the **ph-oton / Photon** framework (Bootstrap 4, server-side HTML generation — not template-based). Key servlet mappings:

| Path | Servlet/Filter | Purpose |
|------|---------------|---------|
| `/*` | `SMPRestFilter` | REST API dispatch |
| `/secure/*` | `SecureApplicationServlet` | Authenticated management UI |
| `/public/*` | `PublicApplicationServlet` | Public info pages |
| `/smp-status/*` | `SMPStatusServlet` | Health/status endpoint |
| `/ping/*` | `PingPongServlet` | Liveness probe |
| `/logout/*` | `SMPLogoutServlet` | Logout |

UI page classes extend `AbstractSMPWebPageForm` or `AbstractSMPWebPageSimpleForm`.

## SQL Backend Details

- **Connection pooling**: Apache Commons DBCP2, configured via `SMPJDBCConfiguration` / `SMPDataSourceProvider`
- **Migrations**: Flyway, scripts in `phoss-smp-backend-sql/src/main/resources/db/migrate-{mysql,postgresql,oracle,db2}/`
- **Supported databases**: MySQL, PostgreSQL, Oracle, DB2 (set via `target-database` property)
- Manager classes are named `SMP*ManagerJDBC` and live in `com.helger.phoss.smp.backend.sql.mgr`

Connection pool properties (v8.0.11+):
```properties
jdbc.pooling.max-connections = 8
jdbc.pooling.max-wait.millis = 10000
jdbc.pooling.between-evictions-runs.millis = 300000
jdbc.pooling.min-evictable-idle.millis = 1800000
```

## Configuration

Primary config file: `src/main/resources/application.properties` (in each webapp module). For local overrides use `private-application.properties` (gitignored).

For production deployments, override the config file path with: `-Dconfig.file=/path/to/application.properties`

Key properties:

```properties
# Backend selection
smp.backend = sql   # or: xml, mongodb

# Identifier scheme
smp.identifiertype = peppol   # peppol, peppol-lax, simple, bdxr1, bdxr2
smp.rest.type = peppol         # peppol or bdxr

# PKI — keystore must contain exactly one certificate; key and keystore passwords must match
smp.keystore.type = pkcs12     # jks, pkcs12, or bcfks
smp.keystore.path = keystore/smp.p12
smp.keystore.password = ...
smp.keystore.key.alias = ...
smp.keystore.key.password = ...

# SQL backend
jdbc.driver = com.mysql.cj.jdbc.Driver
jdbc.url = jdbc:mysql://localhost:3306/smp?...
jdbc.user = smp
jdbc.password = smp
target-database = MySQL        # MySQL, PostgreSQL, Oracle, DB2

# Data directory (use absolute path in production)
webapp.datapath = /var/smp

# Mode flags
global.debug = false           # Set true only in development
global.production = true

# Security (recommended for production)
webapp.startpage.participants.none = true
webapp.security.login.errordetails = false
csp.enabled = true
smp.rest.remote.queryapi.disabled = true
smp.status.enabled = false     # /smp-status/ endpoint; off by default

# Reverse proxy support
smp.forceroot = false          # Set true when behind a reverse proxy stripping context path
smp.publicurl = https://smp.example.org
smp.publicurl.mode = request   # request, x-forwarded-header, forwarded-header

# SML integration
sml.enabled = false
sml.smpid = ...

# Directory integration
smp.directory.integration.enabled = false
smp.directory.integration.autoupdate = false

# Human-readable name mappings for UI
webapp.nicename.doctypes.path = /path/to/doctypes.xml
webapp.nicename.processes.path = /path/to/processes.xml
```

### Encoded Slash Handling

Service Group identifiers may contain encoded slashes (`%2F`). Tomcat requires:
```xml
<!-- server.xml Connector -->
encodedSolidusHandling="decode"
```
For Tomcat < 10: set JVM property `org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true`.

## Technology Stack

- **Java 17+**, Maven 3.6+
- **Helger ecosystem**: `ph-commons`, `ph-web`, `ph-oton` (web/UI framework), `ph-db-jdbc`
- **Peppol**: `peppol-commons`, `peppol-smp-client`, `peppol-sml-client`, `peppol-directory-businesscard`
- **Persistence**: Apache Commons DBCP2, Flyway 12, MongoDB driver 5.6.3
- **Logging**: Log4j 2 + SLF4J
- **Testing**: JUnit 4
- **Build plugins**: Felix Bundle Plugin (OSGi), ph-jscompress, ph-csscompress
- **App server**: Tomcat 10.1.x or Jetty 12.x (Jakarta EE 10)
