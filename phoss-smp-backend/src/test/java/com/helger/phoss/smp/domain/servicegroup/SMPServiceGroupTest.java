/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.servicegroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.w3c.dom.Document;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.bdxr.smp1.BDXR1ExtensionConverter;
import com.helger.peppol.bdxr.smp1.marshal.BDXR1MarshallerServiceGroupType;
import com.helger.peppol.smp.marshal.SMPMarshallerServiceGroupType;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xsds.bdxr.smp1.ExtensionType;

/**
 * Test class for class {@link SMPServiceGroup}.
 *
 * @author Philip Helger
 */
public final class SMPServiceGroupTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testBasic ()
  {
    final IParticipantIdentifier aPI = SMPMetaManager.getIdentifierFactory ()
                                                     .createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                   "0088:dummy");
    final SMPServiceGroup aSG = new SMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID, aPI, "<foobar />");
    assertTrue (StringHelper.hasText (aSG.getID ()));
    assertEquals (CSecurity.USER_ADMINISTRATOR_ID, aSG.getOwnerID ());
    assertEquals (aPI, aSG.getParticpantIdentifier ());
    assertEquals ("[{\"Any\":\"<foobar />\"}]", aSG.getExtensionAsString ());

    final com.helger.peppol.smp.ServiceGroupType aSGPeppol = aSG.getAsJAXBObjectPeppol ();
    assertNotNull (aSGPeppol.getExtension ());
    aSGPeppol.setServiceMetadataReferenceCollection (new com.helger.peppol.smp.ServiceMetadataReferenceCollectionType ());

    final Document aDoc = new SMPMarshallerServiceGroupType ().getAsDocument (aSGPeppol);
    assertNotNull (aDoc);
  }

  @Test
  public void testBDXRExtension ()
  {
    final IParticipantIdentifier aPI = SMPMetaManager.getIdentifierFactory ()
                                                     .createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                   "0088:dummy");

    final ExtensionType aExt = new ExtensionType ();
    // The extension "any" MUST be in a different namespace that is not empty
    aExt.setAny (DOMReader.readXMLDOM ("<foobar1 xmlns='abc'/>").getDocumentElement ());
    final ExtensionType aExt2 = new ExtensionType ();
    aExt2.setExtensionID ("xyz");
    aExt2.setAny (DOMReader.readXMLDOM ("<foobar2 xmlns='def'/>").getDocumentElement ());

    // Must be an array!
    final SMPServiceGroup aSG = new SMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID,
                                                     aPI,
                                                     BDXR1ExtensionConverter.convertToString (new CommonsArrayList <> (aExt,
                                                                                                                       aExt2)));
    assertTrue (StringHelper.hasText (aSG.getID ()));
    assertEquals (CSecurity.USER_ADMINISTRATOR_ID, aSG.getOwnerID ());
    assertEquals (aPI, aSG.getParticpantIdentifier ());
    assertNotNull (aSG.getExtensionAsString ());

    final com.helger.xsds.bdxr.smp1.ServiceGroupType aSGBDXR = aSG.getAsJAXBObjectBDXR1 ();
    aSGBDXR.setServiceMetadataReferenceCollection (new com.helger.xsds.bdxr.smp1.ServiceMetadataReferenceCollectionType ());
    assertEquals (2, aSGBDXR.getExtension ().size ());

    final Document aDoc = new BDXR1MarshallerServiceGroupType ().getAsDocument (aSGBDXR);
    assertNotNull (aDoc);
  }
}
