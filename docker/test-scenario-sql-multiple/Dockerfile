#
# Copyright (C) 2015-2026 Philip Helger and contributors
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
FROM tomcat:10.1-jre25

# Use non-blocking random
ENV CATALINA_OPTS="--enable-native-access=ALL-UNNAMED -Djava.security.egd=file:/dev/urandom"

# Special encoded slash handling for SMP
#   Tomcat 10 removed: org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true
#   See: https://tomcat.apache.org/migration-10.html#encoded_slash
# Second sed command adds a Valve to the Host element to suppress server info and error report
RUN  sed -i 's/<Connector port="8080" protocol="HTTP\/1.1"/<Connector port="8080" protocol="HTTP\/1.1" encodedSolidusHandling="passthrough"/' ${CATALINA_HOME}/conf/server.xml \
  && sed -i 's/^.*<\/Host>/<Valve className="org.apache.catalina.valves.ErrorReportValve" showReport="false" showServerInfo="false" \/>\n&/' ${CATALINA_HOME}/conf/server.xml  

# Disable the HotSpot Base64 intrinsic as mitigation for CVE-2026-114938 ("Java2Shell")
ENV CATALINA_OPTS="$CATALINA_OPTS -XX:-UseBASE64Intrinsics"

ARG SMP_VERSION
ENV SMP_VERSION=$SMP_VERSION
LABEL vendor="Philip Helger"
LABEL version=$SMP_VERSION

COPY smp-binary/ $CATALINA_HOME/webapps/ROOT
RUN rm $CATALINA_HOME/webapps/ROOT/WEB-INF/classes/private-application.properties \
 && rm $CATALINA_HOME/webapps/ROOT/WEB-INF/classes/application.properties
COPY config/* $CATALINA_HOME/webapps/ROOT/WEB-INF/classes/
