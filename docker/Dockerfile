# Use an official Tomcat runtime as a base image
FROM tomcat:8.5

# Install vim
RUN apt-get update \
  && apt-get install -y vim \
  && rm -rf /var/lib/apt/lists/*

# Remove predefined Tomcat webapps
RUN rm -r $CATALINA_HOME/webapps/ROOT \
  && rm -r $CATALINA_HOME/webapps/docs \
  && rm -r $CATALINA_HOME/webapps/examples

# Set arguments and labels after initial cleanup was performed :)
# Minimum version for this layout is 5.0.1
ARG VERSION=5.0.3
# Set to environment to be persistent
ENV SMP_VERSION=$VERSION
LABEL vendor="Philip Helger"
LABEL version=$SMP_VERSION
 
# Download, unzip and copy result to Tomcat webapps dir
WORKDIR /home/git
RUN echo Downloading phoss SMP $SMP_VERSION \
  && wget -nv https://github.com/phax/peppol-smp-server/releases/download/peppol-smp-server-parent-pom-$SMP_VERSION/peppol-smp-server-webapp-xml-$SMP_VERSION.war -O smp.zip \ 
  && unzip smp.zip -d smp/ \
  && cp -r smp/ $CATALINA_HOME/webapps/ROOT \
  && rm smp.zip
