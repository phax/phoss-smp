/*
 * Copyright (C) 2014-2023 Philip Helger and contributors
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

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.string.StringHelper;
import com.helger.commons.timing.StopWatch;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.doctype.PeppolDocumentTypeIdentifier;
import com.helger.peppolid.peppol.participant.PeppolParticipantIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.peppolid.peppol.process.PeppolProcessIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.photon.security.CSecurity;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.smpclient.peppol.utils.W3CEndpointReferenceHelper;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.web.scope.mock.WebScopeTestRule;
import com.helger.xsds.peppol.smp1.EndpointType;
import com.helger.xsds.peppol.smp1.ObjectFactory;
import com.helger.xsds.peppol.smp1.ProcessListType;
import com.helger.xsds.peppol.smp1.ProcessType;
import com.helger.xsds.peppol.smp1.ServiceEndpointList;
import com.helger.xsds.peppol.smp1.ServiceInformationType;
import com.helger.xsds.peppol.smp1.ServiceMetadataType;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;

/**
 * Create one million endpoints. Run this AFTER
 * {@link MainCreateManyServiceGroups}.
 *
 * @author Philip Helger
 */
public final class MainCreateManyEndpoints
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainCreateManyEndpoints.class);
  private static final BasicAuthClientCredentials CREDENTIALS = new BasicAuthClientCredentials (CSecurity.USER_ADMINISTRATOR_EMAIL,
                                                                                                CSecurity.USER_ADMINISTRATOR_PASSWORD);

  private static void _testResponseJerseyClient (@Nonnull final Response aResponseMsg,
                                                 @Nonempty final int... aStatusCodes)
  {
    final String sResponse = aResponseMsg.readEntity (String.class);
    if (StringHelper.hasText (sResponse))
      LOGGER.error ("HTTP Response: " + sResponse);
    if (!ArrayHelper.contains (aStatusCodes, aResponseMsg.getStatus ()))
      throw new IllegalStateException (aResponseMsg.getStatus () + " is not in " + Arrays.toString (aStatusCodes));
  }

  @SuppressWarnings ("deprecation")
  public static void main (final String [] args) throws Throwable
  {
    final String sServerBasePath = "http://localhost:90";
    final WebScopeTestRule aRule = new WebScopeTestRule ();
    aRule.before ();
    try
    {
      final StopWatch aSWOverall = StopWatch.createdStarted ();
      final ObjectFactory aObjFactory = new ObjectFactory ();

      for (final EPredefinedDocumentTypeIdentifier aEDT : new EPredefinedDocumentTypeIdentifier [] { EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30,
                                                                                                     EPredefinedDocumentTypeIdentifier.CREDITNOTE_EN16931_PEPPOL_V30,
                                                                                                     EPredefinedDocumentTypeIdentifier.CROSSINDUSTRYINVOICE_CEN_EU_EN16931_2017,
                                                                                                     EPredefinedDocumentTypeIdentifier.XRECHNUNG_INVOICE_UBL_V12,
                                                                                                     EPredefinedDocumentTypeIdentifier.XRECHNUNG_CREDIT_NOTE_UBL_V12,
                                                                                                     EPredefinedDocumentTypeIdentifier.XRECHNUNG_INVOICE_CII_V12,
                                                                                                     EPredefinedDocumentTypeIdentifier.XRECHNUNG_INVOICE_UBL_V20,
                                                                                                     EPredefinedDocumentTypeIdentifier.XRECHNUNG_CREDIT_NOTE_UBL_V20,
                                                                                                     EPredefinedDocumentTypeIdentifier.XRECHNUNG_INVOICE_CII_V202,
                                                                                                     EPredefinedDocumentTypeIdentifier.XRECHNUNG_EXTENSION_INVOICE_UBL_V20,
                                                                                                     EPredefinedDocumentTypeIdentifier.XRECHNUNG_EXTENSION_CREDIT_NOTE_UBL_V20,
                                                                                                     EPredefinedDocumentTypeIdentifier.XRECHNUNG_EXTENSION_INVOICE_CII_V202 })

      {
        final PeppolDocumentTypeIdentifier aDT = aEDT.getAsDocumentTypeIdentifier ();
        final String sDT = aDT.getURIEncoded ();
        final PeppolProcessIdentifier aProcID = EPredefinedProcessIdentifier.BIS3_BILLING.getAsProcessIdentifier ();

        for (int i = 0; i < 10_000; ++i)
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
                           .target (sServerBasePath)
                           .path (sPI)
                           .path ("services")
                           .path (sDT)
                           .request ()
                           .header (CHttpHeader.AUTHORIZATION, CREDENTIALS.getRequestValue ())
                           .delete ();

            // Create a new
            final Response aResponseMsg = ClientBuilder.newClient ()
                                                       .target (sServerBasePath)
                                                       .path (sPI)
                                                       .path ("services")
                                                       .path (sDT)
                                                       .request ()
                                                       .header (CHttpHeader.AUTHORIZATION,
                                                                CREDENTIALS.getRequestValue ())
                                                       .put (Entity.xml (aObjFactory.createServiceMetadata (aSM)));
            _testResponseJerseyClient (aResponseMsg, 200);
          }

          aSW.stop ();
          LOGGER.info (sPI + " took " + aSW.getMillis () + " ms");
        }
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
