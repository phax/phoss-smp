# peppol-smp-server-lightweight

A complete PEPPOL SMP server. Compared to the regular PEPPOL SMP server implementation, this implementation does not use a database as its backend but a file-based store. This makes it much simpler to deploy. The reason to do this is that the data amount in an SMP is usually very small (at last a few thousand entries) and that amount can easily be kept in main memory.

Current version: *3.1.0* but version *4.0.0-SNAPSHOT* provides many advantages and should be considered!

Please provide the classpath to your PEPPOL SMP keystore and the required passwords updating the `smp-server.properties` file in the `src/main/resource` folder. Typically you will place the keystore.jks file in a folder that you will add to your applications server classpath.

#Functionality
Besides the required SMP REST service (as specified by PEPPOL) this application offers (since version 4) a nice user interface where you can maintain your service groups, service information and redirects.

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


Example of a development `smp-server.properties` file (for easy testing):
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
```

Example of a production-like `smp-server.properties` (for close to production setup):

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
# SML URL (incl. the service name) - change on June 9th
sml.url=https://sml.peppolcentral.org/manageparticipantidentifier
```

---

On Twitter: <a href="https://twitter.com/philiphelger">Follow @philiphelger</a>
