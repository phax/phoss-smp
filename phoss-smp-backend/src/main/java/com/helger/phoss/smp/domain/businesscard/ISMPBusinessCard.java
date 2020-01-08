/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.businesscard;

import java.io.Serializable;
import java.util.Comparator;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.id.IHasID;
import com.helger.pd.businesscard.v3.PD3BusinessCardType;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;

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
public interface ISMPBusinessCard extends IHasID <String>, Serializable
{
  /**
   * @return The service group which this business card should handle.
   */
  @Nonnull
  ISMPServiceGroup getServiceGroup ();

  /**
   * @return The ID of the service group to which this business card belongs.
   *         Never <code>null</code>.
   */
  @Nonnull
  @Nonempty
  default String getServiceGroupID ()
  {
    return getServiceGroup ().getID ();
  }

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
    return Comparator.comparing (ISMPBusinessCard::getServiceGroupID);
  }
}
