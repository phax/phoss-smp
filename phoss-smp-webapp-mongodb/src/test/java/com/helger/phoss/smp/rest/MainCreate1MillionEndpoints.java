/*
 * Copyright (C) 2019-2025 Philip Helger and contributors
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
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.doctype.PeppolDocumentTypeIdentifier;
import com.helger.peppolid.peppol.participant.PeppolParticipantIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.peppolid.peppol.process.PeppolProcessIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.backend.mongodb.PhotonSecurityManagerFactoryMongoDB;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.smpclient.peppol.utils.W3CEndpointReferenceHelper;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xsds.peppol.smp1.EndpointType;
import com.helger.xsds.peppol.smp1.ObjectFactory;
import com.helger.xsds.peppol.smp1.ProcessListType;
import com.helger.xsds.peppol.smp1.ProcessType;
import com.helger.xsds.peppol.smp1.ServiceEndpointList;
import com.helger.xsds.peppol.smp1.ServiceInformationType;
import com.helger.xsds.peppol.smp1.ServiceMetadataType;

import jakarta.annotation.Nonnull;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;

/**
 * Create one million endpoints. Run this AFTER
 * {@link MainCreate1MillionServiceGroups}.
 *
 * @author Philip Helger
 */
public final class MainCreate1MillionEndpoints
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainCreate1MillionEndpoints.class);
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

  @SuppressWarnings ("resource")
  public static void main (final String [] args) throws Throwable
  {
    final SMPServerRESTTestRule aRule = new SMPServerRESTTestRule (new FileSystemResource ("src/test/resources/test-smp-server-mongodb.properties"));
    aRule.before ();
    try
    {
      // Set the special PhotonSecurityManager factory
      PhotonSecurityManager.setFactory (new PhotonSecurityManagerFactoryMongoDB ());
      final ObjectFactory aObjFactory = new ObjectFactory ();
      final PeppolDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getAsDocumentTypeIdentifier ();
      final String sDT = aDT.getURIEncoded ();
      final PeppolProcessIdentifier aProcID = EPredefinedProcessIdentifier.BIS3_BILLING.getAsProcessIdentifier ();

      final StopWatch aSWOverall = StopWatch.createdStarted ();
      for (int i = 639276; i < 1_000_000; ++i)
      {
        final StopWatch aSW = StopWatch.createdStarted ();
        final PeppolParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:test-philip-" +
                                                                                                                               StringHelper.getLeadingZero (i,
                                                                                                                                                            7));
        final String sPI = aPI.getURIEncoded ();

        final ServiceMetadataType aSM = new ServiceMetadataType ();
        final ServiceInformationType aSI = new ServiceInformationType ();
        aSI.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI));
        aSI.setDocumentIdentifier (aDT);
        {
          final ProcessListType aPL = new ProcessListType ();
          final ProcessType aProcess = new ProcessType ();
          aProcess.setProcessIdentifier (aProcID);
          final ServiceEndpointList aSEL = new ServiceEndpointList ();
          final EndpointType aEndpoint = new EndpointType ();
          aEndpoint.setEndpointReference (W3CEndpointReferenceHelper.createEndpointReference ("http://test.smpserver/as2"));
          aEndpoint.setRequireBusinessLevelSignature (false);
          aEndpoint.setCertificate ("blacert");
          aEndpoint.setServiceDescription ("Unit test service");
          aEndpoint.setTechnicalContactUrl ("https://github.com/phax/phoss-smp");
          aEndpoint.setTransportProfile (ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2.getID ());
          aSEL.addEndpoint (aEndpoint);
          aProcess.setServiceEndpointList (aSEL);
          aPL.addProcess (aProcess);
          aSI.setProcessList (aPL);
        }
        aSM.setServiceInformation (aSI);

        try (final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
        {
          // Delete old - don't care about the result
          if (false)
            ClientBuilder.newClient ()
                         .target (aRule.getFullURL ())
                         .path (sPI)
                         .path ("services")
                         .path (sDT)
                         .request ()
                         .header (CHttpHeader.AUTHORIZATION, CREDENTIALS.getRequestValue ())
                         .delete ();

          // Create a new
          final Response aResponseMsg = ClientBuilder.newClient ()
                                                     .target (aRule.getFullURL ())
                                                     .path (sPI)
                                                     .path ("services")
                                                     .path (sDT)
                                                     .request ()
                                                     .header (CHttpHeader.AUTHORIZATION, CREDENTIALS.getRequestValue ())
                                                     .put (Entity.xml (aObjFactory.createServiceMetadata (aSM)));
          _testResponseJerseyClient (aResponseMsg, 200);
        }

        aSW.stop ();
        LOGGER.info (sPI + " took " + aSW.getMillis () + " ms");
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
