/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
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

import com.helger.annotation.Nonempty;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroElement;
import com.helger.xml.microdom.convert.IMicroTypeConverter;
import com.helger.xml.microdom.convert.MicroTypeConverter;
import com.helger.xml.microdom.util.MicroHelper;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * This class is internally used to convert {@link SMPServiceGroup} from and to XML.
 *
 * @author Philip Helger
 */
public final class SMPServiceGroupMicroTypeConverter implements IMicroTypeConverter <SMPServiceGroup>
{
  private static final String ATTR_OWNER_ID = "ownerid";
  private static final String ELEMENT_PARTICIPANT_ID = "participant";
  private static final String ELEMENT_EXTENSION = "extension";

  @Nonnull
  public IMicroElement convertToMicroElement (@Nonnull final SMPServiceGroup aValue,
                                              @Nullable final String sNamespaceURI,
                                              @Nonnull @Nonempty final String sTagName)
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

  @Nonnull
  public static SMPServiceGroup convertToNative (@Nonnull final IMicroElement aElement,
                                                 @Nonnull final Function <String, IUser> aOwningUserProvider)
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

  @Nonnull
  public SMPServiceGroup convertToNative (@Nonnull final IMicroElement aElement)
  {
    return convertToNative (aElement, PhotonSecurityManager.getUserMgr ()::getUserOfID);
  }
}
