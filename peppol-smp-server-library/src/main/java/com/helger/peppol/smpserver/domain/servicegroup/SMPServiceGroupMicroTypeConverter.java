/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.peppol.identifier.generic.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.user.ISMPUser;
import com.helger.peppol.smpserver.domain.user.ISMPUserProvider;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.convert.MicroTypeConverter;
import com.helger.xml.microdom.util.MicroHelper;

/**
 * This class is internally used to convert {@link SMPServiceGroup} from and to
 * XML.
 *
 * @author Philip Helger
 */
public final class SMPServiceGroupMicroTypeConverter implements IMicroTypeConverter
{
  private static final String ATTR_OWNER_ID = "ownerid";
  private static final String ELEMENT_PARTICIPANT_ID = "participant";
  private static final String ELEMENT_EXTENSION = "extension";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final Object aObject,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull @Nonempty final String sTagName)
  {
    final ISMPServiceGroup aValue = (ISMPServiceGroup) aObject;
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_OWNER_ID, aValue.getOwnerID ());
    aElement.appendChild (MicroTypeConverter.convertToMicroElement (aValue.getParticpantIdentifier (),
                                                                    sNamespaceURI,
                                                                    ELEMENT_PARTICIPANT_ID));
    if (aValue.hasExtension ())
      aElement.appendElement (sNamespaceURI, ELEMENT_EXTENSION).appendText (aValue.getExtensionAsString ());
    return aElement;
  }

  @Nonnull
  public static ISMPServiceGroup convertToNative (@Nonnull final IMicroElement aElement,
                                                  @Nonnull final ISMPUserProvider aUserProvider)
  {
    final String sOwnerID = aElement.getAttributeValue (ATTR_OWNER_ID);
    final ISMPUser aOwner = aUserProvider.getUserOfID (sOwnerID);
    if (aOwner == null)
      throw new IllegalStateException ("Failed to resolve user ID '" + sOwnerID + "'");

    final SimpleParticipantIdentifier aParticipantIdentifier = MicroTypeConverter.convertToNative (aElement.getFirstChildElement (ELEMENT_PARTICIPANT_ID),
                                                                                                   SimpleParticipantIdentifier.class);
    if (aParticipantIdentifier == null)
      throw new IllegalStateException ("Failed to parse participant identifier " +
                                       MicroHelper.getChildTextContent (aElement, ELEMENT_PARTICIPANT_ID));

    final String sExtension = MicroHelper.getChildTextContentTrimmed (aElement, ELEMENT_EXTENSION);

    // Use the new ID in case the ID was changed!
    return new SMPServiceGroup (aOwner.getID (), aParticipantIdentifier, sExtension);
  }

  @Nonnull
  public ISMPServiceGroup convertToNative (@Nonnull final IMicroElement aElement)
  {
    return convertToNative (aElement, SMPMetaManager.getUserMgr ());
  }
}
