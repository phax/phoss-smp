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
package com.helger.peppol.smpserver.domain.businesscard;

import java.util.Collection;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;

/**
 * Manager for {@link ISMPBusinessCard} objects. Business card objects require a
 * service group to be present first.
 * <p>
 * The files in this package are licensed under Apache 2.0 license
 * </p>
 *
 * @author Philip Helger
 */
public interface ISMPBusinessCardManager
{
  /**
   * @return The callbacks for the business card manager. Never
   *         <code>null</code>.
   * @since 5.0.4
   */
  @Nonnull
  @ReturnsMutableObject
  CallbackList <ISMPBusinessCardCallback> bcCallbacks ();

  /**
   * Create or update a business card for a service group.
   *
   * @param aServiceGroup
   *        Service group the redirect belongs to. May not be <code>null</code>.
   * @param aEntities
   *        The entities for this business card. May not be <code>null</code>.
   * @return The new or updated {@link ISMPBusinessCard}. <code>null</code> if
   *         persistence failed.
   */
  @Nullable
  ISMPBusinessCard createOrUpdateSMPBusinessCard (@Nonnull ISMPServiceGroup aServiceGroup,
                                                  @Nonnull Collection <SMPBusinessCardEntity> aEntities);

  /**
   * Delete the passed SMP business card.
   *
   * @param aSMPBusinessCard
   *        The SMP redirect to be deleted. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if the deletion was successful
   */
  @Nonnull
  EChange deleteSMPBusinessCard (@Nullable ISMPBusinessCard aSMPBusinessCard);

  /**
   * @return All contained SMP business cards. Never <code>null</code> but maybe
   *         empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <ISMPBusinessCard> getAllSMPBusinessCards ();

  /**
   * Get the business card of the passed service group.
   *
   * @param aServiceGroup
   *        The service group to use. May be <code>null</code>.
   * @return The contained business card or <code>null</code> if none is
   *         assigned.
   */
  @Nullable
  ISMPBusinessCard getSMPBusinessCardOfServiceGroup (@Nullable ISMPServiceGroup aServiceGroup);

  /**
   * Get the business card of the passed ID (= Service group ID).
   *
   * @param sID
   *        The ID to use. May be <code>null</code>.
   * @return The contained business card or <code>null</code> if none is
   *         assigned.
   */
  @Nullable
  ISMPBusinessCard getSMPBusinessCardOfID (@Nullable String sID);

  /**
   * @return The count of all contained business cards. Always &ge; 0.
   */
  @Nonnegative
  int getSMPBusinessCardCount ();
}
