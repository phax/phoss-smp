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

import java.io.Serializable;
import java.util.Comparator;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.domain.extension.ISMPHasExtension;

/**
 * Base interface for a single SMP service group.
 *
 * @author Philip Helger
 */
@MustImplementEqualsAndHashcode
public interface ISMPServiceGroup extends IHasID <String>, Serializable, ISMPHasExtension
{
  /**
   * @return the URI encoded participant identifier is the ID.
   */
  @Nonnull
  @Nonempty
  String getID ();

  /**
   * @return The ID of the owning user of this service group. Never
   *         <code>null</code>.
   */
  @Nonnull
  @Nonempty
  String getOwnerID ();

  /**
   * @return The participant identifier of this service group. Never
   *         <code>null</code>.
   */
  @Nonnull
  IParticipantIdentifier getParticpantIdentifier ();

  /**
   * @return This service information object as a PEPPOL SMP JAXB object for the
   *         REST interface. Never <code>null</code>.
   */
  @Nonnull
  com.helger.peppol.smp.ServiceGroupType getAsJAXBObjectPeppol ();

  /**
   * @return This service information object as a BDXR SMP JAXB object for the
   *         REST interface. Never <code>null</code>.
   */
  @Nonnull
  com.helger.peppol.bdxr.ServiceGroupType getAsJAXBObjectBDXR ();

  @Nonnull
  static Comparator <ISMPServiceGroup> comparator ()
  {
    return Comparator.comparing (ISMPServiceGroup::getID);
  }
}
