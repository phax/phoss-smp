# Use an official Tomcat runtime as a base image
FROM tomcat:8.5

# Install Maven and Git
RUN apt-get update \
  && apt-get install -y git maven \
  && rm -rf /var/lib/apt/lists/*

# Remove predefined Tomcat webapps
RUN ls $CATALINA_HOME/webapps
