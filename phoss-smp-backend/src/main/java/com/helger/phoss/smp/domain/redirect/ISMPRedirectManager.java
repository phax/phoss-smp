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

import com.helger.annotation.CheckForSigned;
import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.paging.IPagingSpec;
import com.helger.phoss.smp.domain.SMPTableColumnHelper;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;

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
   * Get a single "page" of all entries matching the provided search text. This method is meant to
   * be used for server side pagination in combination with {@link #getSMPRedirectCount(String)}.<br>
   * The sort fields of the paging specification are resolved via {@link ESMPRedirectColumn} - unknown or
   * non-sortable field names are ignored, because they are provided by a client. If no sort field
   * remains, the first column of {@link ESMPRedirectColumn} is used, so that consecutive page requests return
   * disjunct results.
   *
   * @param aPagingSpec
   *        The paging specification to be applied. May not be <code>null</code>.
   * @param sSearchText
   *        The global search text to filter by. May be <code>null</code> or empty in which case no
   *        filtering takes place. It is matched against all searchable columns of
   *        {@link ESMPRedirectColumn}, ignoring case.
   * @return A non-<code>null</code> but maybe empty list.
   * @since 8.2.1
   */
  @NonNull
  @ReturnsMutableCopy
  default ICommonsList <ISMPRedirect> getAllSMPRedirects (@NonNull final IPagingSpec aPagingSpec,
                                                          @Nullable final String sSearchText)
  {
    return SMPTableColumnHelper.getPage (ESMPRedirectColumn.values (), getAllSMPRedirects (), aPagingSpec, sSearchText);
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
  default long getSMPRedirectCount (@Nullable final String sSearchText)
  {
    if (StringHelper.isEmpty (sSearchText))
      return getSMPRedirectCount ();

    return SMPTableColumnHelper.getCount (ESMPRedirectColumn.values (), getAllSMPRedirects (), sSearchText);
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
