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
package com.helger.phoss.smp.domain.businesscard;

import java.util.Comparator;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.id.IHasID;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.businesscard.v3.PD3BusinessCardType;
import com.helger.peppolid.IParticipantIdentifier;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * This interface represents a single SMP business card for a certain service
 * group.
 * <p>
 * The files in this package are licensed under Apache 2.0 license
 * </p>
 *
 * @author Philip Helger
 */
@MustImplementEqualsAndHashcode
public interface ISMPBusinessCard extends IHasID <String>
{
  /**
   * @return The participant ID of the service group to which this business card
   *         belongs. Never <code>null</code>.
   */
  @Nonnull
  IParticipantIdentifier getParticipantIdentifier ();

  /**
   * @return A copy of all {@link SMPBusinessCardEntity} objects. Never
   *         <code>null</code>.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <SMPBusinessCardEntity> getAllEntities ();

  /**
   * Get the business card entity at the specified index.
   *
   * @param nIndex
   *        The index to query. Should be &ge; 0.
   * @return <code>null</code> if no such entity exists.
   */
  @Nullable
  SMPBusinessCardEntity getEntityAtIndex (@Nonnegative int nIndex);

  /**
   * @return The number of contained entities. Always &ge; 0.
   */
  @Nonnegative
  int getEntityCount ();

  /**
   * @return This business card as a JAXB object for the REST interface. Never
   *         <code>null</code>.
   */
  @Nonnull
  PD3BusinessCardType getAsJAXBObject ();

  @Nonnull
  static Comparator <ISMPBusinessCard> comparator ()
  {
    return Comparator.comparing (ISMPBusinessCard::getID);
  }
}
