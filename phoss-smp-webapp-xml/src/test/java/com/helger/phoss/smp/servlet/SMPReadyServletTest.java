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
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.helger.io.resource.FileSystemResource;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Test class for {@link SMPReadyServlet}.
 *
 * @author vinit-thummar
 */
public final class SMPReadyServletTest
{
  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (new FileSystemResource ("src/test/resources/test-smp-server-xml-peppol.properties"));

  @Test
  public void testXMLBackendIsReady ()
  {
    try (final Client aClient = ClientBuilder.newClient ();
         final Response aResponse = aClient.target (m_aRule.getFullURL ()).path (SMPReadyServlet.SERVLET_DEFAULT_NAME).request ().get ())
    {
      assertEquals (200, aResponse.getStatus ());
      assertTrue (MediaType.APPLICATION_JSON_TYPE.isCompatible (aResponse.getMediaType ()));
      assertEquals ("{\"ready\":true}", aResponse.readEntity (String.class));
    }
  }
}
