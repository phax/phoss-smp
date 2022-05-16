#
# Copyright (C) 2015-2022 Philip Helger (www.helger.com)
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
FROM tomcat:9-jdk11

ARG SMP_VERSION
ENV SMP_VERSION=${SMP_VERSION:-5.7.0}
LABEL vendor="Philip Helger"
LABEL version=$SMP_VERSION

# Special encoded slash handling for SMP
# Use non-blocking random
ENV CATALINA_OPTS="$CATALINA_OPTS -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true -Djava.security.egd=file:/dev/urandom"

COPY smp-binary/ $CATALINA_HOME/webapps/ROOT
RUN rm $CATALINA_HOME/webapps/ROOT/WEB-INF/classes/private-smp-server.properties \
 && rm $CATALINA_HOME/webapps/ROOT/WEB-INF/classes/smp-server.properties
COPY config/* $CATALINA_HOME/webapps/ROOT/WEB-INF/classes/
