# phoss-smp Copilot Instructions

This repository contains the source code for **phoss SMP**, a PEPOLL and OASIS BDXR compliant Service Metadata Publisher.

## Project Structure & Architecture

-   **Multi-Module Maven Project**:
    -   `phoss-smp-backend`: Core domain logic, interfaces, and shared utilities.
    -   `phoss-smp-backend-xml`: XML-based persistence implementation (flat file).
    -   `phoss-smp-backend-sql`: RDBMS persistence implementation (via JDBC/Flyway).
    -   `phoss-smp-backend-mongodb`: MongoDB persistence implementation.
    -   `phoss-smp-webapp`: The web application shell, providing the REST API and Management UI.
    -   `phoss-smp-webapp-{xml,sql,mongodb}`: module bundles for specific deployment targets.

-   **Core Architecture**:
    -   **Manager Pattern**: Business logic is encapsulated in "Managers" (e.g., `ISMPServiceGroupManager`, `ISMPRedirectManager`).
    -   **Central Singleton**: `com.helger.phoss.smp.domain.SMPMetaManager` is the global entry point affecting all managers.
        -   *Usage*: `SMPMetaManager.getServiceGroupMgr()` to access service group operations.
    -   **Backend Agnosticism**: The core uses `ISMPManagerProvider` to obtain manager instances. The concrete provider is selected at startup based on configuration.
    -   **SPI Registration**: Backends register via `com.helger.phoss.smp.backend.ISMPBackendRegistrarSPI` using the standard Java ServiceLoader mechanism.

## Implemented Specifications & Standards

The application is a dual-stack implementation of:
1.  **Peppol SMP Specifications** (v1 & v2)
2.  **OASIS BDXR SMP Specifications** (v1 & v2)

-   **Configuration Switch**: The behavior is controlled via `SMPServerConfiguration`:
    -   `smp.identifiertype`: defines the syntax of identifiers (Peppol vs BDXR).
    -   `smp.rest.type`: defines the REST API structure exposed to clients.
-   **Key Enums**:
    -   `ESMPRESTType`: Defines the API flavor (PEPPOL, BDXR).
    -   `ESMPIdentifierType` (via `peppol-id` library): Defines the identifier format.

-   **Web Application**:
    -   **Entry Point**: `com.helger.phoss.smp.servlet.SMPWebAppListener` initializes the system.
    -   **Callbacks**: Changes in domain objects trigger callbacks (e.g., `SMPServiceInformationDirectoryAutoUpdateCallback`) to notify external systems like the Peppol Directory.

## Developer Workflows

-   **Build**:
    -   Use `mvn clean install` to build all modules.
    -   Java 21 is required (see Dockerfiles).

-   **Docker Build**:
    -   Maintainer scripts are in `docker/`.
    -   `docker/build-all.sh` builds images for all backends (XML, SQL, MongoDB) using `docker buildx`.
    -   Dockerfiles: `Dockerfile-release-binary-xml`, `Dockerfile-release-binary-sql`, etc.

-   **Testing**:
    -   Standard **JUnit 4** tests reside in `src/test/java`.
    -   `phoss-smp-backend` contains core domain tests.
    -   Mocking is minimal; logic is often tested with in-memory implementations or files.

## Code Conventions & Patterns

-   **Utilities**: The project heavily relies on `com.helger.commons`.
    -   Use `ICommonsList`, `CommonsArrayList`, `ICommonsMap` instead of standard JDK collection interfaces where appropriate.
    -   Use `StringHelper` for string manipulations.
-   **Configuration**:
    -   Config is handled via `com.helger.config` (files, environment variables).
    -   Key configuration constants are in `com.helger.phoss.smp.config.SMPServerConfiguration`.
    -   Backend selection key: `smp.backend` (values: `xml`, `sql`, `mongodb`).
-   **Logging**: Use SLF4J (`org.slf4j.Logger`).
-   **Nullability**: Use `org.jspecify.annotations` (`@Nullable`, `@NonNull`).
-   **Formatting**: Coding style is "Philip Helger's Personal Style" (brace on new line, specific whitespace). Mimic existing file formatting.

## Key Integration Points

-   **Peppol Directory**: Integrated via `PDClient` (in `phoss-smp-search-indexer` libraries usually, accessed here via `PDClientProvider`).
-   **SML (Service Metadata Locator)**: Configured via `ISMLInfoManager`.

## Application Startup Flow

1.  Container starts (Tomcat).
2.  `SMPWebAppListener.contextInitialized` is called.
3.  Configuration is loaded (`SMPConfigProvider`).
4.  Backend is selected via `smp.backend` property.
5.  `SMPBackendRegistry` instantiates the corresponding `ISMPManagerProvider`.
6.  `SMPMetaManager` is initialized with the provider.
7.  Web UI and REST API become available.
