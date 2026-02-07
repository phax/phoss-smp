/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.servicegroup;

import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.MicroQName;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.convert.MicroTypeConverter;
import com.helger.xml.microdom.util.MicroHelper;

/**
 * This class is internally used to convert {@link SMPServiceGroup} from and to XML.
 *
 * @author Philip Helger
 */
public final class SMPServiceGroupMicroTypeConverter implements IMicroTypeConverter <SMPServiceGroup>
{
  private static final MicroQName ATTR_OWNER_ID = new MicroQName ("ownerid");
  private static final String ELEMENT_PARTICIPANT_ID = "participant";
  private static final String ELEMENT_EXTENSION = "extension";

  @NonNull
  public IMicroElement convertToMicroElement (@NonNull final SMPServiceGroup aValue,
                                              @Nullable final String sNamespaceURI,
                                              @NonNull @Nonempty final String sTagName)
  {
    final IMicroElement aElement = new MicroElement (sNamespaceURI, sTagName);
    aElement.setAttribute (ATTR_OWNER_ID, aValue.getOwnerID ());
    aElement.addChild (MicroTypeConverter.convertToMicroElement (aValue.getParticipantIdentifier (),
                                                                 sNamespaceURI,
                                                                 ELEMENT_PARTICIPANT_ID));
    if (aValue.getExtensions ().extensions ().isNotEmpty ())
      aElement.addElementNS (sNamespaceURI, ELEMENT_EXTENSION)
              .addText (aValue.getExtensions ().getExtensionsAsJsonString ());
    return aElement;
  }

  @NonNull
  public static SMPServiceGroup convertToNative (@NonNull final IMicroElement aElement,
                                                 @NonNull final Function <String, IUser> aOwningUserProvider)
  {
    final String sOwnerID = aElement.getAttributeValue (ATTR_OWNER_ID);
    final IUser aOwner = aOwningUserProvider.apply (sOwnerID);
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

  @NonNull
  public SMPServiceGroup convertToNative (@NonNull final IMicroElement aElement)
  {
    return convertToNative (aElement, PhotonSecurityManager.getUserMgr ()::getUserOfID);
  }
}
