# peppol-smp-server
A complete PEPPOL SMP server with a database backend (MySQL by default).

Current version: *3.1.0*

Please provide the classpath to your PEPPOL SMP keystore and the required passwords updating the `smp-server.properties` in the `src/main/resource` folder. Typically you will place the keystore.jks file in a folder that you will add to your applications server classpath. 

#Setting up Apache Tomcat
Tomcat must be set up with the following java system property:
`org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH = true`

This can be done by adding:
`-Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH="true"`
as a JVM argument (Tomcat Properties -> Java -> Java Options) or
put it into the `catalina.sh` in Linux: 
```
JAVA_OPTS="$JAVA_OPTS -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true"
```

SQL file to create the database from a backup is available in the `database_backups` folder.

#Configuring the SMP server
The service is configured using a single configuration file `src/main/resources/smp-server.properties`. The following list describes all the possible configuration items:

  * **`smp.keystore.path`**: The classpath - relative to the project - where the Java key store (of type JKS) with the SMP certificate is located. An empty directory `src/main/resources/keystore` is present which could contain the key store. In this case the properties entry should start with `keystore/`.
    **Note:** The key store should contain exactly one certificate entry with an arbitrary name and the certificate must have the same password as the whole key store!
  * **`smp.keystore.password`**: The password used to access the key store.
  * **`smp.keystore.key.alias`**: The alias of the key within the key store. Is case sensitive and may not be empty. This alias is used to sign certain response messages.
  * **`smp.keystore.key.password`**: The password of the key with the above specified alias. Should be the same as the password of the whole key store (see `smp.keystore.password`).
  * **`smp.forceroot`**: It indicates, whether all internal paths should be forced to root ("/").
    This is a flag which may either have the value true or false.
    This is especially helpful, when the application runs in a Tomcat application context (e.g. "/smp") but is proxied to a different domain via Apache httpd.
  * **`sml.active`**: This field indicates, whether connection to the SML is active or not.
    This is a flag which may either have the value `true` or `false`.
    For testing purposes you may set it to `false` to disable the communication with the SML. For production the value must be `true` so that all relevant adds, updates or deletes of participants is communicated to the SML which will create the respective DNS entries.
  * **`sml.smpid`**: The SMP ID to use when using the SML interface.
    **Note:** it must be the same ID that was used for the initial registration of the SMP to the SML.
    **Note:** is only required if the entry `sml.active` is set to `true`.
  * **`sml.url`**: The URL of the SML manage business identifier service. For production purposes (SML) use `https://sml.peppolcentral.org/manageparticipantidentifier`. For the test-SML (SMK) use the URL `https://smk.peppolcentral.org/manageparticipantidentifier`.
    **Note:** is only required if the entry `sml.active` is set to `true`.
    **Note:** this is the field that needs to be changed for the SML migration on June 9th 2015! 
  * **`jdbc.driver`**: The JDBC driver class to be used by JPA. For MySQL use `com.mysql.jdbc.Driver`.
  * **`jdbc.url`**: The JDBC URL of the database to connect to. For a local MySQL database called "smp" the string would look like this: `jdbc:mysql://localhost:3306/smp?autoReconnect=true`
    **Note:** the URL depends on the JDBC driver used!
  * **`jdbc.user`**: The database user to be used when connecting to the database.
  * **`jdbc.password`**: The password of the JDBC user to be used when connecting to the DB
  * **`target-database`**: The JPA target database type to be used. For MySQL this value should be MySQL
    **Note:** Please see the documentation of EclipseLink for other target database systems!
  * **`jdbc.read-connections.max`**: The maximum number of JDBC connections to be used for reading. Usually 10 should be suitable for most use cases. 

Example of a development `smp-server.properties` file using a local MySQL database called smp without an SML connector (for easy testing):
```
## Keystore data
smp.keystore.path         = keystore/keystore.jks
smp.keystore.password     = peppol
smp.keystore.key.alias    = smp keypair
smp.keystore.key.password = peppol

# Force all paths to be "/" instead of the context path 
smp.forceroot = true

## Write to SML? true or false
sml.active=false

## JDBC configuration for DB
jdbc.driver = com.mysql.jdbc.Driver
jdbc.url = jdbc:mysql://localhost:3306/smp
jdbc.user = smp
jdbc.password = smp
target-database = MySQL
jdbc.read-connections.max = 10
```

Example of a production-like `smp-server.properties` file using a local MySQL database called smp with the SML connector (for close to production setup):

```
## Keystore data
smp.keystore.path         = keystore/keystore.jks
smp.keystore.password     = peppol
smp.keystore.key.alias    = smp keypair
smp.keystore.key.password = peppol

# Force all paths to be "/" instead of the context path 
smp.forceroot = true

## Write to SML? true or false
sml.active=true
# SMP ID
sml.smpid=TEST-SMP-ID1
# SML URL (incl. the service name) - to be changed on June 9th
sml.url=https://sml.peppolcentral.org/manageparticipantidentifier

## JDBC configuration for DB
jdbc.driver = com.mysql.jdbc.Driver
jdbc.url = jdbc:mysql://localhost:3306/smp
jdbc.user = smp
jdbc.password = smp
target-database = MySQL
jdbc.read-connections.max = 10
```

---

On Twitter: <a href="https://twitter.com/philiphelger">Follow @philiphelger</a>
