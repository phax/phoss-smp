package com.helger.peppol.smpserver.domain.redirect;

import java.util.Collection;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.state.EChange;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;

public interface ISMPRedirectManager
{
  /**
   * Create or update a redirect for a service group.
   *
   * @param aServiceGroup
   *        Service group
   * @param aDocumentTypeIdentifier
   *        Document type identifier affected.
   * @param sTargetHref
   *        Target URL of the new SMP
   * @param sSubjectUniqueIdentifier
   *        The subject unique identifier of the target SMPs certificate used to
   *        sign its resources.
   * @param sExtension
   *        Optional extension element
   * @return The new or updated {@link ISMPRedirect}. Never <code>null</code>.
   */
  @Nonnull
  ISMPRedirect createSMPRedirect (@Nonnull ISMPServiceGroup aServiceGroup,
                                  @Nonnull IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                  @Nonnull String sTargetHref,
                                  @Nonnull String sSubjectUniqueIdentifier,
                                  @Nullable String sExtension);

  @Nonnull
  EChange deleteSMPRedirect (@Nullable ISMPRedirect aSMPRedirect);

  @Nonnull
  EChange deleteAllSMPRedirectsOfServiceGroup (@Nullable ISMPServiceGroup aServiceGroup);

  @Nonnull
  EChange deleteAllSMPRedirectsOfServiceGroup (@Nullable String sServiceGroupID);

  @Nonnull
  @ReturnsMutableCopy
  Collection <? extends ISMPRedirect> getAllSMPRedirects ();

  @Nonnull
  @ReturnsMutableCopy
  Collection <? extends ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable ISMPServiceGroup aServiceGroup);

  @Nonnull
  @ReturnsMutableCopy
  Collection <? extends ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable String sServiceGroupID);

  @Nonnegative
  int getSMPRedirectCount ();

  @Nullable
  ISMPRedirect getSMPRedirectOfServiceGroupAndDocumentType (@Nullable ISMPServiceGroup aServiceGroup,
                                                            @Nullable IDocumentTypeIdentifier aDocTypeID);

  @Nullable
  ISMPRedirect getSMPRedirectOfServiceGroupAndDocumentType (@Nullable String sServiceGroupID,
                                                            @Nullable IDocumentTypeIdentifier aDocTypeID);

  @Nullable
  ISMPRedirect getSMPRedirectOfID (@Nullable String sID);

  boolean containsSMPRedirectWithID (@Nullable String sID);
}
