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
package com.helger.phoss.smp.domain.redirect;

import java.security.cert.X509Certificate;
import java.util.Comparator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPPagingHelper;

/**
 * Manager for {@link ISMPRedirect} objects. Redirect objects require a service
 * group to be present first.
 *
 * @author Philip Helger
 */
public interface ISMPRedirectManager
{
  /**
   * @return The comparator to be used for the server side pagination, to ensure a stable order
   *         across single page requests.
   */
  @NonNull
  private static Comparator <ISMPRedirect> _getDefaultComparator ()
  {
    return Comparator.comparing (ISMPRedirect::getServiceGroupID)
                     .thenComparing (x -> x.getDocumentTypeIdentifier ().getURIEncoded ());
  }

  /**
   * @return A non-<code>null</code> mutable list of callbacks.
   */
  @NonNull
  @ReturnsMutableObject
  CallbackList <ISMPRedirectCallback> redirectCallbacks ();

  /**
   * Create or update a redirect for a service group.
   *
   * @param aParticipantID
   *        Service group participant ID the redirect belongs to. May not be
   *        <code>null</code>.
   * @param aDocumentTypeIdentifier
   *        Document type identifier effected. May not be <code>null</code>.
   * @param sTargetHref
   *        Target URL of the new SMP. May neither be <code>null</code> nor
   *        empty.
   * @param sSubjectUniqueIdentifier
   *        The subject unique identifier of the target SMPs certificate used to
   *        sign its resources. May neither be <code>null</code> nor empty.
   * @param aCertificate
   *        The certificate of the target SMP. Required for OASIS BDXR SMP v2
   *        May be <code>null</code>.
   * @param sExtension
   *        Optional extension element. May be <code>null</code>. If present it
   *        must be well-formed XML content.
   * @return The new or updated {@link ISMPRedirect}. <code>null</code> if
   *         persistence failed.
   */
  @Nullable
  ISMPRedirect createOrUpdateSMPRedirect (@NonNull IParticipantIdentifier aParticipantID,
                                          @NonNull IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                          @NonNull @Nonempty String sTargetHref,
                                          @NonNull @Nonempty String sSubjectUniqueIdentifier,
                                          @Nullable X509Certificate aCertificate,
                                          @Nullable String sExtension);

  /**
   * Delete the passed SMP redirect.
   *
   * @param aSMPRedirect
   *        The SMP redirect to be deleted. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if the deletion was successful
   */
  @NonNull
  EChange deleteSMPRedirect (@Nullable ISMPRedirect aSMPRedirect);

  /**
   * Delete all redirects owned by the passed service groups.-
   *
   * @param aParticipantID
   *        The service group ID which is about to be deleted.
   * @return {@link EChange#CHANGED} is something was deleted
   */
  @NonNull
  EChange deleteAllSMPRedirectsOfServiceGroup (@Nullable IParticipantIdentifier aParticipantID);

  /**
   * @return All contained SMP redirects. Never <code>null</code> but maybe
   *         empty.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <ISMPRedirect> getAllSMPRedirects ();

  /**
   * Get a single "page" of all redirects, sorted by service group ID and document type ID. This
   * method is meant to be used for server side pagination. Backends that support native paging
   * should override this method.
   *
   * @param nStartIndex
   *        The 0-based index of the first redirect to be returned. Must be &ge; 0.
   * @param nMaxCount
   *        The maximum number of redirects to be returned. Must be &ge; 0.
   * @return A non-<code>null</code> but maybe empty list of redirects.
   * @since 8.2.1
   */
  @NonNull
  @ReturnsMutableCopy
  default ICommonsList <ISMPRedirect> getAllSMPRedirects (@Nonnegative final int nStartIndex,
                                                          @Nonnegative final int nMaxCount)
  {
    return SMPPagingHelper.getPage (getAllSMPRedirects (), _getDefaultComparator (), nStartIndex, nMaxCount);
  }

  /**
   * Get a single "page" of all redirects matching the provided search text, sorted by service group
   * ID and document type ID. This method is meant to be used for server side pagination in
   * combination with {@link #getSMPRedirectCount(String)}.
   *
   * @param sSearchText
   *        The search text to filter the redirects. May be <code>null</code> or empty in which case
   *        no filtering takes place.
   * @param nStartIndex
   *        The 0-based index of the first matching redirect to be returned. Must be &ge; 0.
   * @param nMaxCount
   *        The maximum number of redirects to be returned. Must be &ge; 0.
   * @return A non-<code>null</code> but maybe empty list of redirects.
   * @since 8.2.1
   */
  @NonNull
  @ReturnsMutableCopy
  default ICommonsList <ISMPRedirect> getAllSMPRedirects (@Nullable final String sSearchText,
                                                          @Nonnegative final int nStartIndex,
                                                          @Nonnegative final int nMaxCount)
  {
    if (StringHelper.isEmpty (sSearchText))
      return getAllSMPRedirects (nStartIndex, nMaxCount);

    return SMPPagingHelper.getPage (getAllSMPRedirects ().getAll (x -> isMatchingSearchText (x, sSearchText)),
                                    _getDefaultComparator (),
                                    nStartIndex,
                                    nMaxCount);
  }

  /**
   * Get all redirects of the passed service group.
   *
   * @param aParticipantID
   *        The service group ID to use. May be <code>null</code>.
   * @return All contained SMP redirects for the passed service group. Never
   *         <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  ICommonsList <ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable IParticipantIdentifier aParticipantID);

  /**
   * @return The count of all contained redirects. Always &ge; 0.
   */
  @Nonnegative
  long getSMPRedirectCount ();

  /**
   * Get the number of redirects matching the provided search text.
   *
   * @param sSearchText
   *        The search text to filter the redirects. May be <code>null</code> or empty in which case
   *        all redirects are counted.
   * @return The count of all matching redirects. Always &ge; 0.
   * @since 8.2.1
   */
  @Nonnegative
  default long getSMPRedirectCount (@Nullable final String sSearchText)
  {
    if (StringHelper.isEmpty (sSearchText))
      return getSMPRedirectCount ();

    return getAllSMPRedirects ().getCount (x -> isMatchingSearchText (x, sSearchText));
  }

  /**
   * Check if the provided redirect matches the provided search text. The participant ID, the
   * document type ID and the target URL are checked.
   *
   * @param aRedirect
   *        The redirect to check. May not be <code>null</code>.
   * @param sSearchText
   *        The search text to be searched. May be <code>null</code> or empty in which case
   *        <code>true</code> is returned.
   * @return <code>true</code> if the redirect matches, <code>false</code> if not.
   * @since 8.2.1
   */
  static boolean isMatchingSearchText (@NonNull final ISMPRedirect aRedirect, @Nullable final String sSearchText)
  {
    if (StringHelper.isEmpty (sSearchText))
      return true;

    return SMPPagingHelper.matchesSearchText (aRedirect.getServiceGroupID (), sSearchText) ||
           SMPPagingHelper.matchesSearchText (aRedirect.getDocumentTypeIdentifier ().getURIEncoded (), sSearchText) ||
           SMPPagingHelper.matchesSearchText (aRedirect.getTargetHref (), sSearchText);
  }

  /**
   * Find the redirect that matches the passed tuple of service group and
   * document type.
   *
   * @param aParticipantID
   *        The service group ID to query. May be <code>null</code>.
   * @param aDocTypeID
   *        The document type to query. May be <code>null</code>.
   * @return <code>null</code> if the passed service group is <code>null</code>
   *         or not contained, or if the passed document type is
   *         <code>null</code> or if it is not contained as a redirect in the
   *         passed service group.
   */
  @Nullable
  ISMPRedirect getSMPRedirectOfServiceGroupAndDocumentType (@Nullable IParticipantIdentifier aParticipantID,
                                                            @Nullable IDocumentTypeIdentifier aDocTypeID);
}
