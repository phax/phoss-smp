/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.servlet;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.helger.io.resource.FileSystemResource;
import com.helger.mime.CMimeType;
import com.helger.phoss.smp.mock.MockHttpClient;
import com.helger.phoss.smp.mock.MockHttpResponse;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;

/**
 * Test class for {@link SMPReadyServlet}.
 *
 * @author vinit-thummar
 */
public final class SMPReadyServletTest
{
  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (new FileSystemResource ("src/test/resources/test-smp-server-xml-peppol.properties"));

  private MockHttpClient m_aClient;

  @Before
  public void before ()
  {
    m_aClient = new MockHttpClient (m_aRule.getFullURL ());
  }

  @After
  public void after ()
  {
    m_aClient.close ();
  }

  @Test
  public void testXMLBackendIsReady ()
  {
    final MockHttpResponse aResponse = m_aClient.get (SMPReadyServlet.SERVLET_DEFAULT_NAME);
    assertEquals (200, aResponse.getStatusCode ());
    assertEquals (CMimeType.APPLICATION_JSON.getAsString (), aResponse.getMimeType ());
    assertEquals ("{\"ready\":true}", aResponse.getBodyAsString ());
  }
}
