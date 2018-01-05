/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.domain.servicegroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.bdxr.BDXRExtensionConverter;
import com.helger.peppol.bdxr.ExtensionType;
import com.helger.peppol.bdxr.marshal.BDXRMarshallerServiceGroupType;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.peppol.PeppolIdentifierHelper;
import com.helger.peppol.smp.marshal.SMPMarshallerServiceGroupType;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.mock.SMPServerTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.xml.serialize.read.DOMReader;

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

    final Document aDoc = new SMPMarshallerServiceGroupType ().getAsDocument (aSGPeppol);
    assertNotNull (aDoc);
  }

  @Test
  public void testBDXRExtension () throws SAXException
  {
    final IParticipantIdentifier aPI = SMPMetaManager.getIdentifierFactory ()
                                                     .createParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                                   "0088:dummy");

    final ExtensionType aExt = new ExtensionType ();
    aExt.setAny (DOMReader.readXMLDOM ("<foobar/>").getDocumentElement ());
    final ExtensionType aExt2 = new ExtensionType ();
    aExt2.setExtensionID ("xyz");
    aExt2.setAny (DOMReader.readXMLDOM ("<foobar/>").getDocumentElement ());

    // Must be an array!
    final SMPServiceGroup aSG = new SMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID,
                                                     aPI,
                                                     BDXRExtensionConverter.convertToString (new CommonsArrayList<> (aExt,
                                                                                                                     aExt2)));
    assertTrue (StringHelper.hasText (aSG.getID ()));
    assertEquals (CSecurity.USER_ADMINISTRATOR_ID, aSG.getOwnerID ());
    assertEquals (aPI, aSG.getParticpantIdentifier ());
    assertNotNull (aSG.getExtensionAsString ());

    final com.helger.peppol.bdxr.ServiceGroupType aSGBDXR = aSG.getAsJAXBObjectBDXR ();
    assertEquals (2, aSGBDXR.getExtension ().size ());

    final Document aDoc = new BDXRMarshallerServiceGroupType ().getAsDocument (aSGBDXR);
    assertNotNull (aDoc);
  }
}
