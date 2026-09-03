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
package com.helger.phoss.smp.domain.businesscard;

import java.util.Collection;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.collection.paging.IPagingSpec;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPTableColumnHelper;

/**
 * Manager for {@link ISMPBusinessCard} objects. Business card objects require a service group to be
 * present first.
 * <p>
 * The files in this package are licensed under Apache 2.0 license
 * </p>
 *
 * @author Philip Helger
 */
public interface ISMPBusinessCardManager
{
  /**
   * @return The callbacks for the business card manager. Never <code>null</code>.
   * @since 5.0.4
   */
  @NonNull
  @ReturnsMutableObject
  CallbackList <ISMPBusinessCardCallback> bcCallbacks ();

  /**
   * Create or update a business card for a service group.
   *
   * @param aParticipantID
   *        Participant ID the business card belongs to. May not be <code>null</code>.
   * @param aEntities
   *        The entities for this business card. May not be <code>null</code>.
   * @param bSyncToDirectory
   *        <code>true</code> to synchronize the change to the remote directory, <code>false</code>
   *        to disable it
   * @return The new or updated {@link ISMPBusinessCard}. <code>null</code> if persistence failed.
   */
  @Nullable
  ISMPBusinessCard createOrUpdateSMPBusinessCard (@NonNull final IParticipantIdentifier aParticipantID,
                                                  @NonNull Collection <SMPBusinessCardEntity> aEntities,
                                                  boolean bSyncToDirectory);

  /**
   * Delete the passed SMP business card.
   *
   * @param aSMPBusinessCard
   *        The SMP redirect to be deleted. May be <code>null</code>.
   * @param bSyncToDirectory
   *        <code>true</code> to synchronize the change to the remote directory, <code>false</code>
   *        to disable it
   * @return {@link EChange#CHANGED} if the deletion was successful
   */
  @NonNull
  EChange deleteSMPBusinessCard (@Nullable ISMPBusinessCard aSMPBusinessCard, boolean bSyncToDirectory);

  /**
   * @return All contained SMP business cards. Never <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <ISMPBusinessCard> getAllSMPBusinessCards ();

  /**
   * Get a single "page" of all entries matching the provided search text. This method is meant to
   * be used for server side pagination in combination with
   * {@link #getSMPBusinessCardCount(String)}.<br>
   * The sort fields of the paging specification are resolved via {@link ESMPBusinessCardColumn} -
   * unknown or non-sortable field names are ignored, because they are provided by a client. If no
   * sort field remains, the first column of {@link ESMPBusinessCardColumn} is used, so that
   * consecutive page requests return disjunct results.
   *
   * @param aPagingSpec
   *        The paging specification to be applied. May not be <code>null</code>.
   * @param sSearchText
   *        The global search text to filter by. May be <code>null</code> or empty in which case no
   *        filtering takes place. It is matched against all searchable columns of
   *        {@link ESMPBusinessCardColumn}, ignoring case.
   * @return A non-<code>null</code> but maybe empty list.
   * @since 8.2.1
   */
  @NonNull
  @ReturnsMutableCopy
  default ICommonsList <ISMPBusinessCard> getAllSMPBusinessCards (@NonNull final IPagingSpec aPagingSpec,
                                                                  @Nullable final String sSearchText)
  {
    return SMPTableColumnHelper.getPage (ESMPBusinessCardColumn.values (),
                                         getAllSMPBusinessCards (),
                                         aPagingSpec,
                                         sSearchText);
  }

  /**
   * @return All contained SMP business card IDs. Never <code>null</code> but maybe empty.
   * @since 5.6.0
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsSet <String> getAllSMPBusinessCardIDs ();

  /**
   * Check if a business card of the passed service group ID exists.
   *
   * @param aID
   *        The ID to check. May be <code>null</code>.
   * @return <code>true</code> if a business card is contained, <code>false</code> if not.
   * @since 7.1.5
   */
  boolean containsSMPBusinessCardOfID (@Nullable IParticipantIdentifier aID);

  /**
   * Get the business card of the passed ID (= Service group ID).
   *
   * @param aID
   *        The ID to use. May be <code>null</code>.
   * @return The contained business card or <code>null</code> if none is assigned.
   */
  @Nullable
  ISMPBusinessCard getSMPBusinessCardOfID (@Nullable IParticipantIdentifier aID);

  /**
   * @return The count of all contained business cards. Always &ge; 0.
   */
  @Nonnegative
  long getSMPBusinessCardCount ();

  /**
   * Get the number of entries matching the provided search text.
   *
   * @param sSearchText
   *        The global search text to filter by. May be <code>null</code> or empty in which case all
   *        entries are counted.
   * @return The number of matching entries. May be &lt; 0 in case there was an error querying (e.g.
   *         because of a missing SQL backend).
   * @since 8.2.1
   */
  @CheckForSigned
  default long getSMPBusinessCardCount (@Nullable final String sSearchText)
  {
    if (StringHelper.isEmpty (sSearchText))
      return getSMPBusinessCardCount ();

    return SMPTableColumnHelper.getCount (ESMPBusinessCardColumn.values (), getAllSMPBusinessCards (), sSearchText);
  }
}
