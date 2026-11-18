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
package com.helger.phoss.smp.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.hc.core5.http.HttpEntity;
import org.jspecify.annotations.NonNull;
import org.junit.Assume;
import org.junit.Test;

import com.helger.base.array.ArrayHelper;
import com.helger.io.resource.FileSystemResource;
import com.helger.io.resource.IReadableResource;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.mock.MockHttpClient;
import com.helger.phoss.smp.mock.MockHttpResponse;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.smpclient.peppol.marshal.SMPMarshallerServiceGroupType;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xsds.peppol.smp1.ServiceGroupType;
import com.helger.xsds.peppol.smp1.ServiceMetadataReferenceCollectionType;

/**
 * Test the read-only mode (configuration property <code>smp.readonly</code>) with the SQL backend.
 * A writable instance creates the test data first, and removes it afterwards.
 *
 * @author Philip Helger
 */
public final class ReadOnlyInterfaceTest extends AbstractSMPWebAppSQLTest
{
  private static final IReadableResource PROPERTIES_FILE_READ_ONLY = new FileSystemResource ("src/test/resources/test-smp-server-sql-read-only.properties");

  @NonNull
  private static HttpEntity _xmlEntity (@NonNull final ServiceGroupType aSG)
  {
    return MockHttpClient.createXMLEntity (new SMPMarshallerServiceGroupType (), aSG);
  }

  private static void _assertStatus (@NonNull final MockHttpResponse aResponse, final int... aStatusCodes)
  {
    assertTrue (aResponse.getStatusCode () + " is not one of the expected status codes",
                ArrayHelper.contains (aStatusCodes, aResponse.getStatusCode ()));
  }

  @Test
  public void testReadOnly () throws Throwable
  {
    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:readonly");
    final String sPI = aPI.getURIPercentEncoded ();

    final ServiceGroupType aSG = new ServiceGroupType ();
    aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI));
    aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

    // Create the test data with a writable instance
    final SMPServerRESTTestRule aWritableRule = new SMPServerRESTTestRule (PROPERTIES_FILE);
    aWritableRule.before ();
    try (final MockHttpClient aClient = new MockHttpClient (aWritableRule.getFullURL ());
         final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
    {
      assertFalse (SMPMetaManager.isReadOnly ());
      final MockHttpResponse aResponse = aClient.put (sPI, CREDENTIALS, _xmlEntity (aSG));
      // Seems like the database is not running
      Assume.assumeTrue (aResponse.getStatusCode () != 500);
      _assertStatus (aResponse, 200);
    }
    finally
    {
      aWritableRule.after ();
    }

    try
    {
      final SMPServerRESTTestRule aReadOnlyRule = new SMPServerRESTTestRule (PROPERTIES_FILE_READ_ONLY);
      aReadOnlyRule.before ();
      try (final MockHttpClient aClient = new MockHttpClient (aReadOnlyRule.getFullURL ());
           final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
      {
        assertTrue (SMPMetaManager.isReadOnly ());

        // Public read APIs work
        _assertStatus (aClient.get (sPI), 200);
        _assertStatus (aClient.get ("businesscard/" + sPI), 404);

        // Writing APIs are not available, even with valid credentials
        _assertStatus (aClient.put (sPI, CREDENTIALS, _xmlEntity (aSG)), 404, 405);
        _assertStatus (aClient.delete (sPI, CREDENTIALS), 404, 405);
        _assertStatus (aClient.delete (sPI + "/services", CREDENTIALS), 404, 405);

        // Authenticated read APIs are not available
        _assertStatus (aClient.get ("servicegroupids/all", CREDENTIALS), 404);
        _assertStatus (aClient.get ("list/" + CREDENTIALS.getUserName (), CREDENTIALS), 404);
        _assertStatus (aClient.get ("exchange/export/all/xml/v1", CREDENTIALS), 404);

        // No management GUI and no generic ph-oton servlets
        _assertStatus (aClient.get ("secure/"), 404);
        _assertStatus (aClient.get ("logout/"), 404);
        _assertStatus (aClient.get ("ajax/dataTables"), 404);
        // "userupload/" alone would be handled as a Service Group ID by the public REST API
        _assertStatus (aClient.get ("userupload/upload/file"), 404);

        // The public start page is still available
        _assertStatus (aClient.get ("public/"), 200);
      }
      finally
      {
        aReadOnlyRule.after ();
      }
    }
    finally
    {
      // Remove the test data with a writable instance
      aWritableRule.before ();
      try (final MockHttpClient aClient = new MockHttpClient (aWritableRule.getFullURL ());
           final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
      {
        assertFalse (SMPMetaManager.isReadOnly ());
        _assertStatus (aClient.delete (sPI, CREDENTIALS), 200);
      }
      finally
      {
        aWritableRule.after ();
      }
    }
  }
}
