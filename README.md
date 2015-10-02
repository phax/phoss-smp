# peppol-smp-server
A complete PEPPOL SMP server. Compared to the CIPA implementation this SMP comes with a management GUI and optionally an XML backend for simplified operations.

Status per 2015-10-02: everything seems to work with both backends. Some fine tuning is still required. 

The new layout is as follows:
  * `peppol-smp-server-library` is the base library with common features for SMP servers
  * `peppol-smp-server-sql` is the SQL backend for the SMP server
  * `peppol-smp-server-xml` is the XML backend for the SMP server
  * `peppol-smp-server-webapp` is the main SMP server web application with a management GUI and the REST service

---

On Twitter: <a href="https://twitter.com/philiphelger">Follow @philiphelger</a>
