![Logo](https://github.com/phax/phoss-smp/blob/master/docs/logo/phoss-smp-272-100.png)

<!-- ph-badge-start -->
[![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.helger/phoss-smp-parent-pom/badge.svg)](https://maven-badges.sml.io/sonatype-central/com.helger/phoss-smp-parent-pom/)
[![javadoc](https://javadoc.io/badge2/com.helger/phoss-smp-backend/javadoc.svg)](https://javadoc.io/doc/com.helger/phoss-smp-backend)

> If this project saved you some time or made your day a little easier, a star would mean a lot — it helps others find it too.
<!-- ph-badge-end -->

phoss SMP is a complete SMP server that supports both the Peppol SMP 1.x specification as well as the OASIS BDXR SMP 1.0 and 2.0 specifications.
It comes with a management GUI and an XML, SQL or MongoDB backend for simplified operations.  

It was the first SMP to be [CEF eDelivery conformant](https://ec.europa.eu/digital-building-blocks/wikis/display/DIGITAL/OASIS+SMP+conformant+solutions).

This project is part of my Peppol solution stack. See https://github.com/phax/peppol for other components and libraries in that area.

Latest version: **[8.4.0](https://github.com/phax/phoss-smp/releases/tag/phoss-smp-parent-pom-8.4.0)** (2026-09-04).
See the special [Migrations guide](https://github.com/phax/phoss-smp/wiki/Migrations) for actions necessary on updates/version changes.

Docker containers can be found, depending on the backend you want to use:
* https://hub.docker.com/r/phelger/phoss-smp-xml/tags (same as https://hub.docker.com/r/phelger/smp/tags)
* https://hub.docker.com/r/phelger/phoss-smp-sql/tags
* https://hub.docker.com/r/phelger/phoss-smp-mongodb/tags

## Documentation

Please read the **[Wiki](https://github.com/phax/phoss-smp/wiki)** for a detailed description, configuration reference and setup hints. It contains an introduction with screenshots, configuration, building and running instructions:

* [News and noteworthy](https://github.com/phax/phoss-smp/wiki/News-and-noteworthy)
* [Migrations](https://github.com/phax/phoss-smp/wiki/Migrations)
* [Download](https://github.com/phax/phoss-smp/wiki/Download)
* [Features](https://github.com/phax/phoss-smp/wiki/Features)
  * [REST API](https://github.com/phax/phoss-smp/wiki/REST-API)
  * [Status API](https://github.com/phax/phoss-smp/wiki/Status-API)
  * [Readiness API](https://github.com/phax/phoss-smp/wiki/Readiness-API)
  * [Custom Properties](https://github.com/phax/phoss-smp/wiki/Custom-Properties)
* [Configuration](https://github.com/phax/phoss-smp/wiki/Configuration)
  * [Certificate setup](https://github.com/phax/phoss-smp/wiki/Certificate-setup)
  * [Custom Nice Names](https://github.com/phax/phoss-smp/wiki/Custom-Nice-Names)
  * [Peppol Directory Integration](https://github.com/phax/phoss-smp/wiki/Peppol-Directory-Integration)
  * [Peppol AS4](https://github.com/phax/phoss-smp/wiki/Peppol-AS4)
* [Running](https://github.com/phax/phoss-smp/wiki/Running)
  * [Peppol initialization](https://github.com/phax/phoss-smp/wiki/Peppol-initialization)
  * [SMP Extensions](https://github.com/phax/phoss-smp/wiki/SMP-Extensions)
  * [Security](https://github.com/phax/phoss-smp/wiki/Security)
  * [Apache httpd](https://github.com/phax/phoss-smp/wiki/Apache-httpd)
  * [nginx](https://github.com/phax/phoss-smp/wiki/nginx)
  * [IIS](https://github.com/phax/phoss-smp/wiki/IIS)
  * [Apache Tomcat](https://github.com/phax/phoss-smp/wiki/Apache-Tomcat)
  * [WildFly](https://github.com/phax/phoss-smp/wiki/WildFly)
  * [SML notes](https://github.com/phax/phoss-smp/wiki/SML-notes)
  * [Docker](https://github.com/phax/phoss-smp/wiki/Docker)
* Source related
  * [Source](https://github.com/phax/phoss-smp/wiki/Source)
  * [Building](https://github.com/phax/phoss-smp/wiki/Building)
  * [Extensions](https://github.com/phax/phoss-smp/wiki/Extensions)
* Other information
  * [AusDigital DCP](https://github.com/phax/phoss-smp/wiki/AusDigital-DCP)
  * [Difference between Peppol and OASIS BDXR](https://github.com/phax/phoss-smp/wiki/Difference-between-Peppol-and-OASIS-BDXR)
  * [Usage in Croatia](https://github.com/phax/phoss-smp/wiki/Usage-in-Croatia)
* [Future plans](https://github.com/phax/phoss-smp/wiki/Future-plans)
* [License](https://github.com/phax/phoss-smp/wiki/License)
* [Release Tasks](https://github.com/phax/phoss-smp/wiki/Release-Tasks)

For a quick start guide to setup an SMP for Peppol: see also the step by step tutorial in https://github.com/phax/phoss-smp/tree/master/docs

If you like (and use) this SMP it is highly appreciated if you could star this project on GitHub - thanks

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.
