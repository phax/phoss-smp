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

import java.util.Comparator;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.base.id.IHasID;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.extension.ISMPHasExtension;

import jakarta.annotation.Nonnull;

/**
 * Base interface for a single SMP service group.
 *
 * @author Philip Helger
 */
@MustImplementEqualsAndHashcode
public interface ISMPServiceGroup extends IHasID <String>, ISMPHasExtension
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
  IParticipantIdentifier getParticipantIdentifier ();

  /**
   * @return This service information object as a Peppol SMP JAXB object for the
   *         REST interface. Never <code>null</code>.
   */
  @Nonnull
  com.helger.xsds.peppol.smp1.ServiceGroupType getAsJAXBObjectPeppol ();

  /**
   * @return This service information object as a BDXR SMP v1 JAXB object for
   *         the REST interface. Never <code>null</code>.
   */
  @Nonnull
  com.helger.xsds.bdxr.smp1.ServiceGroupType getAsJAXBObjectBDXR1 ();

  /**
   * @return This service information object as a BDXR SMP v2 JAXB object for
   *         the REST interface. Never <code>null</code>.
   */
  @Nonnull
  com.helger.xsds.bdxr.smp2.ServiceGroupType getAsJAXBObjectBDXR2 ();

  @Nonnull
  static Comparator <ISMPServiceGroup> comparator ()
  {
    return Comparator.comparing (ISMPServiceGroup::getID);
  }
}
