![Logo](https://github.com/phax/phoss-smp/blob/master/docs/logo/phoss-smp-272-100.png)

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.helger/phoss-smp-parent-pom/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.helger/phoss-smp-parent-pom) 

phoss SMP is a complete SMP server that supports both the PEPPOL SMP specification as well as the OASIS BDXR SMP 1.0 specification.
It comes with a management GUI and an XML backend for simplified operations.  
It was the first SMP to be [CEF eDelivery conformant](https://ec.europa.eu/cefdigital/wiki/display/CEFDIGITAL/OASIS+SMP+conformant+solutions).

Latest version: **[5.6.0](https://github.com/phax/phoss-smp/releases/tag/phoss-smp-parent-pom-5.6.0)** (2022-02-22).
See the special [Migrations guide](https://github.com/phax/phoss-smp/wiki/Migrations) for actions necessary on updates/version changes.

**!! Users with SQL backend need to change the DB layout when updating to 5.3.x !!**

Docker containers can be found, depending on the backend you want to use:
* https://hub.docker.com/r/phelger/phoss-smp-xml/tags (same as https://hub.docker.com/r/phelger/smp/tags)
* https://hub.docker.com/r/phelger/phoss-smp-sql/tags
* https://hub.docker.com/r/phelger/phoss-smp-mongodb/tags

Please read the **[Wiki](https://github.com/phax/phoss-smp/wiki)** for a detailed description, configuration reference and setup hints. It contains an introduction with screenshots, configuration, building and running instructions as well as [News and noteworthy](https://github.com/phax/phoss-smp/wiki/News-and-noteworthy).

For querying an SMP server you may want to have a look at the OSS [peppol-smp-client](https://github.com/phax/peppol-commons/) library.

If you like (and use) this SMP it is highly appreciated if you could star this project on GitHub - thanks

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
On Twitter: <a href="https://twitter.com/philiphelger">@philiphelger</a> |
Kindly supported by [YourKit Java Profiler](https://www.yourkit.com)