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

import java.util.Comparator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.base.id.IHasID;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.extension.ISMPHasExtension;
import com.helger.phoss.smp.domain.sgprops.SGCustomPropertyList;

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
  @NonNull
  @Nonempty
  String getID ();

  /**
   * @return The ID of the owning user of this service group. Never <code>null</code>.
   */
  @NonNull
  @Nonempty
  String getOwnerID ();

  /**
   * @return The participant identifier of this service group. Never <code>null</code>.
   */
  @NonNull
  IParticipantIdentifier getParticipantIdentifier ();

  /**
   * @return <code>true</code> if this service group has custom properties, <code>false</code> if
   *         not.
   * @since 8.1.0
   */
  default boolean hasCustomProperties ()
  {
    return getCustomProperties () != null;
  }

  /**
   * @return The number of contained custom properties. Always &ge; 0.
   * @since 8.1.0
   */
  @Nonnegative
  default int getCustomPropertyCount ()
  {
    final var aList = getCustomProperties ();
    return aList == null ? 0 : aList.size ();
  }

  /**
   * @return The custom properties of this service group. May be <code>null</code>.
   * @since 8.1.0
   */
  @Nullable
  SGCustomPropertyList getCustomProperties ();

  default @Nullable String getCustomPropertyValue (@Nullable final String sCustomPropertyName)
  {
    final SGCustomPropertyList aList = getCustomProperties ();
    return aList == null ? null : aList.getValue (sCustomPropertyName);
  }

  /**
   * @return This service information object as a Peppol SMP JAXB object for the REST interface.
   *         Never <code>null</code>.
   */
  com.helger.xsds.peppol.smp1.@NonNull ServiceGroupType getAsJAXBObjectPeppol ();

  /**
   * @return This service information object as a BDXR SMP v1 JAXB object for the REST interface.
   *         Never <code>null</code>.
   */
  com.helger.xsds.bdxr.smp1.@NonNull ServiceGroupType getAsJAXBObjectBDXR1 ();

  /**
   * @return This service information object as a BDXR SMP v2 JAXB object for the REST interface.
   *         Never <code>null</code>.
   */
  com.helger.xsds.bdxr.smp2.@NonNull ServiceGroupType getAsJAXBObjectBDXR2 ();

  @NonNull
  static Comparator <ISMPServiceGroup> comparator ()
  {
    return Comparator.comparing (ISMPServiceGroup::getID);
  }
}
