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
package com.helger.peppol.smpserver.domain.redirect;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;

/**
 * Manager for {@link ISMPRedirect} objects. Redirect objects require a service
 * group to be present first.
 *
 * @author Philip Helger
 */
public interface ISMPRedirectManager
{
  /**
   * Create or update a redirect for a service group.
   *
   * @param aServiceGroup
   *        Service group the redirect belongs to. May not be <code>null</code>.
   * @param aDocumentTypeIdentifier
   *        Document type identifier effected. May not be <code>null</code>.
   * @param sTargetHref
   *        Target URL of the new SMP. May neither be <code>null</code> nor
   *        empty.
   * @param sSubjectUniqueIdentifier
   *        The subject unique identifier of the target SMPs certificate used to
   *        sign its resources. May neither be <code>null</code> nor empty.
   * @param sExtension
   *        Optional extension element. May be <code>null</code>. If present it
   *        must be well-formed XML content.
   * @return The new or updated {@link ISMPRedirect}. <code>null</code> if
   *         persistence failed.
   */
  @Nullable
  ISMPRedirect createOrUpdateSMPRedirect (@Nonnull ISMPServiceGroup aServiceGroup,
                                          @Nonnull IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                          @Nonnull @Nonempty String sTargetHref,
                                          @Nonnull @Nonempty String sSubjectUniqueIdentifier,
                                          @Nullable String sExtension);

  /**
   * Delete the passed SMP redirect.
   *
   * @param aSMPRedirect
   *        The SMP redirect to be deleted. May be <code>null</code>.
   * @return {@link EChange#CHANGED} if the deletion was successful
   */
  @Nonnull
  EChange deleteSMPRedirect (@Nullable ISMPRedirect aSMPRedirect);

  /**
   * Delete all redirects owned by the passed service groups.-
   *
   * @param aServiceGroup
   *        The service group which is about to be deleted.
   * @return {@link EChange#CHANGED} is something was deleted
   */
  @Nonnull
  EChange deleteAllSMPRedirectsOfServiceGroup (@Nullable ISMPServiceGroup aServiceGroup);

  /**
   * @return All contained SMP redirects. Never <code>null</code> but maybe
   *         empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <ISMPRedirect> getAllSMPRedirects ();

  /**
   * Get all redirects of the passed service group.
   *
   * @param aServiceGroup
   *        The service group to use. May be <code>null</code>.
   * @return All contained SMP redirects for the passed service group. Never
   *         <code>null</code> but maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  ICommonsList <ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable ISMPServiceGroup aServiceGroup);

  /**
   * @return The count of all contained redirects. Always &ge; 0.
   */
  @Nonnegative
  int getSMPRedirectCount ();

  /**
   * Find the redirect that matches the passed tuple of service group and
   * document type.
   *
   * @param aServiceGroup
   *        The service group to query. May be <code>null</code>.
   * @param aDocTypeID
   *        The document type to query. May be <code>null</code>.
   * @return <code>null</code> if the passed service group is <code>null</code>
   *         or not contained, or if the passed document type is
   *         <code>null</code> or if it is not contained as a redirect in the
   *         passed service group.
   */
  @Nullable
  ISMPRedirect getSMPRedirectOfServiceGroupAndDocumentType (@Nullable ISMPServiceGroup aServiceGroup,
                                                            @Nullable IDocumentTypeIdentifier aDocTypeID);
}
