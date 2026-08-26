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
import java.util.Comparator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPPagingHelper;

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
   * Get a single "page" of all contained SMP business cards, sorted by the business card ID. This
   * method is meant to be used for server side pagination. Backends that support native paging
   * should override this method.
   *
   * @param nStartIndex
   *        The 0-based index of the first business card to be returned. Must be &ge; 0.
   * @param nMaxCount
   *        The maximum number of business cards to be returned. Must be &ge; 0.
   * @return A non-<code>null</code> but maybe empty list of business cards.
   * @since 8.2.1
   */
  @NonNull
  @ReturnsMutableCopy
  default ICommonsList <ISMPBusinessCard> getAllSMPBusinessCards (@Nonnegative final int nStartIndex,
                                                                  @Nonnegative final int nMaxCount)
  {
    return SMPPagingHelper.getPage (getAllSMPBusinessCards (),
                                    Comparator.comparing (ISMPBusinessCard::getID),
                                    nStartIndex,
                                    nMaxCount);
  }

  /**
   * Get a single "page" of all SMP business cards matching the provided search text, sorted by the
   * business card ID. This method is meant to be used for server side pagination in combination
   * with {@link #getSMPBusinessCardCount(String)}.
   *
   * @param sSearchText
   *        The search text to filter the business cards. May be <code>null</code> or empty in which
   *        case no filtering takes place.
   * @param nStartIndex
   *        The 0-based index of the first matching business card to be returned. Must be &ge; 0.
   * @param nMaxCount
   *        The maximum number of business cards to be returned. Must be &ge; 0.
   * @return A non-<code>null</code> but maybe empty list of business cards.
   * @since 8.2.1
   */
  @NonNull
  @ReturnsMutableCopy
  default ICommonsList <ISMPBusinessCard> getAllSMPBusinessCards (@Nullable final String sSearchText,
                                                                  @Nonnegative final int nStartIndex,
                                                                  @Nonnegative final int nMaxCount)
  {
    if (StringHelper.isEmpty (sSearchText))
      return getAllSMPBusinessCards (nStartIndex, nMaxCount);

    return SMPPagingHelper.getPage (getAllSMPBusinessCards ().getAll (x -> isMatchingSearchText (x, sSearchText)),
                                    Comparator.comparing (ISMPBusinessCard::getID),
                                    nStartIndex,
                                    nMaxCount);
  }

  /**
   * Check if the provided business card matches the provided search text. The participant ID as
   * well as the names and the country codes of all contained entities are checked.
   *
   * @param aBusinessCard
   *        The business card to check. May not be <code>null</code>.
   * @param sSearchText
   *        The search text to be searched. May be <code>null</code> or empty in which case
   *        <code>true</code> is returned.
   * @return <code>true</code> if the business card matches, <code>false</code> if not.
   * @since 8.2.1
   */
  static boolean isMatchingSearchText (@NonNull final ISMPBusinessCard aBusinessCard,
                                       @Nullable final String sSearchText)
  {
    if (StringHelper.isEmpty (sSearchText))
      return true;

    if (SMPPagingHelper.matchesSearchText (aBusinessCard.getID (), sSearchText))
      return true;

    return aBusinessCard.getAllEntities ()
                        .containsAny (aEntity -> SMPPagingHelper.matchesSearchText (aEntity.getCountryCode (),
                                                                                    sSearchText) ||
                                                 aEntity.names ()
                                                        .containsAny (aName -> SMPPagingHelper.matchesSearchText (aName.getName (),
                                                                                                                  sSearchText)));
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
   * Get the number of business cards matching the provided search text.
   *
   * @param sSearchText
   *        The search text to filter the business cards. May be <code>null</code> or empty in which
   *        case all business cards are counted.
   * @return The count of all matching business cards. Always &ge; 0.
   * @since 8.2.1
   */
  @Nonnegative
  default long getSMPBusinessCardCount (@Nullable final String sSearchText)
  {
    if (StringHelper.isEmpty (sSearchText))
      return getSMPBusinessCardCount ();

    return getAllSMPBusinessCards ().getCount (x -> isMatchingSearchText (x, sSearchText));
  }
}
