/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.rest;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.photon.security.CSecurity;

abstract class AbstractSMPWebAppSQLTest
{
  protected static final BasicAuthClientCredentials CREDENTIALS = true ? new BasicAuthClientCredentials (CSecurity.USER_ADMINISTRATOR_EMAIL,
                                                                                                         CSecurity.USER_ADMINISTRATOR_PASSWORD)
                                                                       : new BasicAuthClientCredentials ("peppol_user", "Test1234");

  protected static final String PID_PREFIX_9999_PHOSS = "9999:phoss";

  protected static final String PROPERTIES_FILE = ClassPathResource.getAsFile ("test-smp-server-sql.properties").getAbsolutePath ();
}
