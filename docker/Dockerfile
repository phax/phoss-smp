# Use an official Tomcat runtime as a base image
FROM tomcat:8.5

ARG SMP_VERSION=5.0.2
LABEL vendor="Philip Helger"
LABEL version=$SMP_VERSION

# Install Java 8 JDK, Maven and Git
RUN apt-get update \
  && apt-get install -y openjdk-8-jdk-headless git maven \
  && rm -rf /var/lib/apt/lists/*

# Remove predefined Tomcat webapps
RUN rm -r $CATALINA_HOME/webapps/ROOT \
  && rm -r $CATALINA_HOME/webapps/docs \
  && rm -r $CATALINA_HOME/webapps/examples
 
# Checkout from git and build
WORKDIR /home/git
RUN git clone https://github.com/phax/peppol-smp-server.git . \
  && git checkout tags/peppol-smp-server-parent-pom-$SMP_VERSION -b work \
  && mvn clean install -DskipTests

# Copy result to Tomcat webapps dir
RUN cp -r peppol-smp-server-webapp-xml/target/peppol-smp-server-webapp-xml-$SMP_VERSION/ $CATALINA_HOME/webapps/ROOT \
  && ls $CATALINA_HOME/webapps/ROOT
