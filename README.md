# peppol-smp-server
A complete PEPPOL SMP server with a database backend (MySQL by default).

Current version: *3.0.0*

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

---

On Twitter: <a href="https://twitter.com/philiphelger">Follow @philiphelger</a>
