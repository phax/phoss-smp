#
# Copyright (C) 2015-2019 Philip Helger (www.helger.com)
# philip[at]helger[dot]com
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Use an official Tomcat runtime as a base image
FROM tomcat:9-jre11

# Special encoded slash handling for SMP
# Use non-blocking random
ENV CATALINA_OPTS="$CATALINA_OPTS -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true -Djava.security.egd=file:/dev/urandom"

# Install vim, Java 11 JDK, Maven and Git
RUN apt-get update \
  && apt-get install -y vim openjdk-11-jdk-headless git maven \
  && rm -rf /var/lib/apt/lists/*

# Remove predefined Tomcat webapps
RUN rm -r $CATALINA_HOME/webapps/ROOT \
  && rm -r $CATALINA_HOME/webapps/docs \
  && rm -r $CATALINA_HOME/webapps/examples

# Set arguments and labels after initial cleanup was performed :)
# Minimum version for this layout is 5.0.1
ARG SMP_VERSION
# Set to environment to be persistent
ENV SMP_VERSION=${SMP_VERSION:-5.1.2}
LABEL vendor="Philip Helger"
LABEL version=$SMP_VERSION
 
# Checkout from git and build
# Note: Up to and including v5.1.2 the tag was called "peppol-smp-server-*" instead of "phoss-smp-*"
WORKDIR /home/git
RUN echo Building phoss SMP $SMP_VERSION \
  && git clone https://github.com/phax/phoss-smp.git . \
  && git checkout tags/phoss-smp-parent-pom-$SMP_VERSION -b work \
  && mvn clean install -DskipTests

# Copy result to Tomcat webapps dir
# Note: Up to and including v5.1.2 the folders were called "peppol-smp-server-*" instead of "phoss-smp-*"
RUN cp -r phoss-smp-webapp-xml/target/phoss-smp-webapp-xml-$SMP_VERSION/ $CATALINA_HOME/webapps/ROOT
