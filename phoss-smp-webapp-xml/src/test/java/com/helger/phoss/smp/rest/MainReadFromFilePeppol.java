/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
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

import java.io.File;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.array.ArrayHelper;
import com.helger.base.string.StringHelper;
import com.helger.base.timing.StopWatch;
import com.helger.http.CHttpHeader;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.io.resource.FileSystemResource;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.photon.security.CSecurity;

import jakarta.annotation.Nonnull;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;

/**
 * Create one million endpoints. Run this AFTER
 * {@link MainCreateManyServiceGroups}.
 *
 * @author Philip Helger
 */
public final class MainReadFromFilePeppol
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainReadFromFilePeppol.class);
  private static final BasicAuthClientCredentials CREDENTIALS = new BasicAuthClientCredentials (CSecurity.USER_ADMINISTRATOR_EMAIL,
                                                                                                CSecurity.USER_ADMINISTRATOR_PASSWORD);

  private static void _testResponseJerseyClient (@Nonnull final Response aResponseMsg,
                                                 @Nonempty final int... aStatusCodes)
  {
    final String sResponse = aResponseMsg.readEntity (String.class);
    if (StringHelper.isNotEmpty (sResponse))
      LOGGER.error ("HTTP Response: " + sResponse);
    if (!ArrayHelper.contains (aStatusCodes, aResponseMsg.getStatus ()))
      throw new IllegalStateException (aResponseMsg.getStatus () + " is not in " + Arrays.toString (aStatusCodes));
  }

  public static void main (final String [] args) throws Throwable
  {
    final SMPServerRESTTestRule aRule = new SMPServerRESTTestRule (new FileSystemResource ("src/test/resources/test-smp-server-xml-peppol.properties"));
    aRule.before ();
    try
    {
      final String sServerBasePath = aRule.getFullURL ();
      final StopWatch aSWOverall = StopWatch.createdStarted ();

      // These values must match the values in the test file
      final IParticipantIdentifier aParticipantID = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9915:xxx");
      final IDocumentTypeIdentifier aDocTypeID = PeppolIdentifierFactory.INSTANCE.createDocumentTypeIdentifierWithDefaultScheme ("urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1");

      // Delete existing ServiceGroup (if exists)
      {
        final Response aResponseMsg = ClientBuilder.newClient ()
                                                   .target (sServerBasePath)
                                                   .path (aParticipantID.getURIEncoded ())
                                                   .queryParam ("delete-in-sml", Boolean.FALSE)
                                                   .request ()
                                                   .header (CHttpHeader.AUTHORIZATION, CREDENTIALS.getRequestValue ())
                                                   .delete ();
        _testResponseJerseyClient (aResponseMsg, 200, 404);
      }

      {
        // Create a new ServiceGroup
        final Response aResponseMsg = ClientBuilder.newClient ()
                                                   .target (sServerBasePath)
                                                   .path (aParticipantID.getURIEncoded ())
                                                   .queryParam ("create-in-sml", Boolean.FALSE)
                                                   .request ()
                                                   .header (CHttpHeader.AUTHORIZATION, CREDENTIALS.getRequestValue ())
                                                   .put (Entity.xml (new File ("src/test/resources/rest-files/peppol-service-group.xml")));
        _testResponseJerseyClient (aResponseMsg, 200);
      }

      {
        // Add endpoint
        final Response aResponseMsg = ClientBuilder.newClient ()
                                                   .target (sServerBasePath)
                                                   .path (aParticipantID.getURIEncoded ())
                                                   .path ("services")
                                                   .path (aDocTypeID.getURIEncoded ())
                                                   .request ()
                                                   .header (CHttpHeader.AUTHORIZATION, CREDENTIALS.getRequestValue ())
                                                   .put (Entity.xml (new File ("src/test/resources/rest-files/peppol-service-metadata.xml")));
        _testResponseJerseyClient (aResponseMsg, 200);
      }

      {
        // Add Business Card
        final Response aResponseMsg = ClientBuilder.newClient ()
                                                   .target (sServerBasePath)
                                                   .path ("businesscard")
                                                   .path (aParticipantID.getURIEncoded ())
                                                   .request ()
                                                   .header (CHttpHeader.AUTHORIZATION, CREDENTIALS.getRequestValue ())
                                                   .put (Entity.xml (new File ("src/test/resources/rest-files/peppol-business-card-v3.xml")));
        _testResponseJerseyClient (aResponseMsg, 200);
      }

      aSWOverall.stop ();
      LOGGER.info ("Overall process took " + aSWOverall.getMillis () + " ms or " + aSWOverall.getDuration ());
    }
    finally
    {
      aRule.after ();
    }
  }
}
