/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
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

import com.helger.commons.collection.ext.CommonsArrayList;
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
