/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.exchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;

import org.jspecify.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardEntity;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardName;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroReader;
import com.helger.xml.microdom.serialize.MicroWriter;

/**
 * Test class for class {@link ServiceGroupExport}. Especially it is verified, that the streaming
 * export creates exactly the same XML as the in-memory export.
 *
 * @author Philip Helger
 */
public final class ServiceGroupExportFuncTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @NonNull
  private static String _getInMemoryExport (@NonNull final ICommonsList <ISMPServiceGroup> aServiceGroups,
                                            final boolean bIncludeBusinessCards)
  {
    final IMicroDocument aDoc = ServiceGroupExport.createExportDataXMLVer10 (aServiceGroups, bIncludeBusinessCards);
    return MicroWriter.getNodeAsString (aDoc, ServiceGroupExport.XML_WRITER_SETTINGS);
  }

  @NonNull
  private static String _getStreamedExport (@NonNull final ICommonsList <ISMPServiceGroup> aServiceGroups,
                                            final boolean bIncludeBusinessCards)
  {
    final Charset aCharset = ServiceGroupExport.XML_WRITER_SETTINGS.getCharset ();
    try (final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ())
    {
      ServiceGroupExport.createExportDataXMLVer10 (aServiceGroups, bIncludeBusinessCards, aBAOS);
      return aBAOS.getAsString (aCharset);
    }
  }

  @Test
  public void testStreamedExportIsIdenticalToInMemoryExport () throws SMPServerException
  {
    // Ensure the user is present
    final IUser aTestUser = PhotonSecurityManager.getUserMgr ().getUserOfID (CSecurity.USER_ADMINISTRATOR_ID);
    assertNotNull (aTestUser);

    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInformationMgr = SMPMetaManager.getServiceInformationMgr ();
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();

    // Deliberately not in alphabetical order, to ensure the sorting is applied
    final IParticipantIdentifier aPI1 = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                        "0088:export2");
    final IParticipantIdentifier aPI2 = aIdentifierFactory.createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                        "0088:export1");
    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI1, true);
    aServiceGroupMgr.deleteSMPServiceGroupNoEx (aPI2, true);

    assertNotNull (aServiceGroupMgr.createSMPServiceGroup (aTestUser.getID (), aPI1, "<ext1 />", null, true));
    try
    {
      assertNotNull (aServiceGroupMgr.createSMPServiceGroup (aTestUser.getID (), aPI2, null, null, true));
      try
      {
        final XMLOffsetDateTime aStartDT = PDTFactory.getCurrentXMLOffsetDateTime ();
        final XMLOffsetDateTime aEndDT = aStartDT.plusYears (1);
        final IProcessIdentifier aProcessID = aIdentifierFactory.createProcessIdentifier (PeppolIdentifierHelper.DEFAULT_PROCESS_SCHEME,
                                                                                          "testproc");
        final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS,
                                                                                                    "xml::xml##testdoctype::1");

        // Service Information with 2 Endpoints in 1 Process
        final SMPEndpoint aEP1 = new SMPEndpoint ("epid1",
                                                  "tp1",
                                                  "http://localhost/as4",
                                                  false,
                                                  "minauth",
                                                  aStartDT,
                                                  aEndDT,
                                                  "cert1",
                                                  "sd",
                                                  "tc",
                                                  "ti",
                                                  "<extep />");
        final SMPEndpoint aEP2 = new SMPEndpoint ("epid2",
                                                  "tp2",
                                                  "http://localhost/as2",
                                                  true,
                                                  "minauth",
                                                  aStartDT,
                                                  aEndDT,
                                                  "cert2",
                                                  "sd",
                                                  "tc",
                                                  "ti",
                                                  null);
        final SMPProcess aProcess = new SMPProcess (aProcessID,
                                                    new CommonsArrayList <> (aEP1, aEP2),
                                                    "<extproc />");
        assertTrue (aServiceInformationMgr.mergeSMPServiceInformation (new SMPServiceInformation (aPI1,
                                                                                                  aDocTypeID,
                                                                                                  new CommonsArrayList <> (aProcess),
                                                                                                  "<extsi />"))
                                          .isSuccess ());

        // A Redirect on the other Service Group
        assertNotNull (aRedirectMgr.createOrUpdateSMPRedirect (aPI2, aDocTypeID, "bla", "foo", null, "<extred />"));

        // A Business Card on one of them
        final SMPBusinessCardEntity aEntity = new SMPBusinessCardEntity ();
        aEntity.names ().add (new SMPBusinessCardName ("Test Name", null));
        aEntity.setCountryCode ("AT");
        assertNotNull (aBusinessCardMgr.createOrUpdateSMPBusinessCard (aPI1,
                                                                       new CommonsArrayList <> (aEntity),
                                                                       false));

        final ICommonsList <ISMPServiceGroup> aAllSGs = aServiceGroupMgr.getAllSMPServiceGroups ();
        assertEquals (2, aAllSGs.size ());

        // Without Business Cards
        final String sInMemory = _getInMemoryExport (aAllSGs, false);
        assertTrue (sInMemory.contains ("<" + CSMPExchange.ELEMENT_SERVICEGROUP));
        assertTrue (sInMemory.contains ("<" + CSMPExchange.ELEMENT_SERVICEINFO));
        assertTrue (sInMemory.contains ("<" + CSMPExchange.ELEMENT_REDIRECT));
        assertFalse (sInMemory.contains ("<" + CSMPExchange.ELEMENT_BUSINESSCARD));
        assertEquals ("The streamed export must be identical to the in-memory export",
                      sInMemory,
                      _getStreamedExport (aAllSGs, false));

        // With Business Cards
        final String sInMemoryBC = _getInMemoryExport (aAllSGs, true);
        assertTrue (sInMemoryBC.contains ("<" + CSMPExchange.ELEMENT_BUSINESSCARD));
        assertEquals ("The streamed export must be identical to the in-memory export",
                      sInMemoryBC,
                      _getStreamedExport (aAllSGs, true));
      }
      finally
      {
        aServiceGroupMgr.deleteSMPServiceGroup (aPI2, true);
      }
    }
    finally
    {
      aServiceGroupMgr.deleteSMPServiceGroup (aPI1, true);
    }
  }

  @Test
  public void testEmptyExport ()
  {
    final ICommonsList <ISMPServiceGroup> aEmpty = new CommonsArrayList <> ();

    // Note: for an empty export the in-memory version creates a self closed root element, whereas
    // the streamed version creates an open and a close tag. Both are semantically identical.
    final IMicroDocument aDocInMemory = MicroReader.readMicroXML (_getInMemoryExport (aEmpty, false));
    final IMicroDocument aDocStreamed = MicroReader.readMicroXML (_getStreamedExport (aEmpty, false));
    assertNotNull (aDocInMemory);
    assertNotNull (aDocStreamed);
    assertEquals (CSMPExchange.ELEMENT_SMP_DATA, aDocStreamed.getDocumentElement ().getTagName ());
    assertEquals (aDocInMemory.getDocumentElement ().getAttributeValue (CSMPExchange.ATTR_VERSION),
                  aDocStreamed.getDocumentElement ().getAttributeValue (CSMPExchange.ATTR_VERSION));
    assertTrue (aDocStreamed.getDocumentElement ().getAllChildElements ().isEmpty ());
  }
}
