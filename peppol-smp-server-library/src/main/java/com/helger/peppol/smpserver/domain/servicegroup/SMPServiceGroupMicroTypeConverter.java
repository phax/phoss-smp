/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
