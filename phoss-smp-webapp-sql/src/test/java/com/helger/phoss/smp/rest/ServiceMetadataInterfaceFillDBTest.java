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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.string.StringHelper;
import com.helger.commons.timing.StopWatch;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.doctype.PeppolDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.peppolid.peppol.process.PeppolProcessIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.mock.MockSMPClient;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.smpclient.exception.SMPClientException;
import com.helger.smpclient.peppol.SMPClient;
import com.helger.smpclient.peppol.utils.W3CEndpointReferenceHelper;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xsds.peppol.smp1.EndpointType;
import com.helger.xsds.peppol.smp1.ProcessListType;
import com.helger.xsds.peppol.smp1.ProcessType;
import com.helger.xsds.peppol.smp1.ServiceEndpointList;
import com.helger.xsds.peppol.smp1.ServiceInformationType;

/**
 * Create a couple of metadata information for the test service groups.
 *
 * @author Philip Helger
 */
@Ignore ("because it requires prerequisites")
public final class ServiceMetadataInterfaceFillDBTest extends AbstractSMPWebAppSQLTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ServiceMetadataInterfaceFillDBTest.class);

  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (PROPERTIES_FILE);

  @Test
  public void testCreateAndDeleteServiceInformationSMPClient () throws SMPClientException
  {
    final SMPClient aSMPClient = new MockSMPClient ();

    final StopWatch aSW = StopWatch.createdStarted ();
    int nEndpoints = 0;
    try (final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
    {
      final int nCount = 2_000;
      for (int i = 0; i < nCount; ++i)
      {
        final IParticipantIdentifier aPI_LC = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme (PID_PREFIX_9999_PHOSS +
                                                                                                                             StringHelper.getLeadingZero (i,
                                                                                                                                                          4));

        final PeppolDocumentTypeIdentifier aDT = EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getAsDocumentTypeIdentifier ();

        final PeppolProcessIdentifier aProcID = EPredefinedProcessIdentifier.BIS3_BILLING.getAsProcessIdentifier ();

        for (final ESMPTransportProfile eTP : new ESMPTransportProfile [] { ESMPTransportProfile.TRANSPORT_PROFILE_AS2,
                                                                            ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2 })
        {
          final ServiceInformationType aSI = new ServiceInformationType ();
          aSI.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI_LC));
          aSI.setDocumentIdentifier (aDT);
          {
            final ProcessListType aPL = new ProcessListType ();
            final ProcessType aProcess = new ProcessType ();
            aProcess.setProcessIdentifier (aProcID);
            final ServiceEndpointList aSEL = new ServiceEndpointList ();
            final EndpointType aEndpoint = new EndpointType ();
            aEndpoint.setEndpointReference (W3CEndpointReferenceHelper.createEndpointReference ("http://test.smpserver/" + eTP.getID ()));
            aEndpoint.setRequireBusinessLevelSignature (false);
            aEndpoint.setCertificate ("blacert");
            aEndpoint.setServiceDescription ("Unit test service");
            aEndpoint.setTechnicalContactUrl ("https://github.com/phax/phoss-smp");
            aEndpoint.setTransportProfile (eTP.getID ());
            aSEL.addEndpoint (aEndpoint);
            aProcess.setServiceEndpointList (aSEL);
            aPL.addProcess (aProcess);
            aSI.setProcessList (aPL);
          }

          aSMPClient.saveServiceInformation (aSI, CREDENTIALS);
          ++nEndpoints;
        }
      }
    }
    aSW.stop ();
    LOGGER.info ("Created " +
                 nEndpoints +
                 " ServiceInformation in " +
                 aSW.getDuration () +
                 " (= " +
                 (aSW.getMillis () / nEndpoints) +
                 " ms/entry)");
  }
}
