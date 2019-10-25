package com.helger.phoss.smp.rest;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.http.basicauth.BasicAuthClientCredentials;

abstract class AbstractSMPWebAppSQLTest
{
  protected static final BasicAuthClientCredentials CREDENTIALS = new BasicAuthClientCredentials ("peppol_user",
                                                                                                  "Test1234");

  protected static final String PID_PREFIX_9999_PHOSS = "9999:phoss";

  protected static final String PROPERTIES_FILE = ClassPathResource.getAsFile ("test-smp-server-sql.properties")
                                                                   .getAbsolutePath ();
}
