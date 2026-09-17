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
package com.helger.phoss.smp.cache;

import org.jspecify.annotations.NonNull;

import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectCallback;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupCallback;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationCallback;

/**
 * A callback implementation that invalidates all cached REST API responses of a participant,
 * whenever a service group, a service information (the endpoints) or a redirect of that participant
 * was created, updated or deleted.
 *
 * @author Philip Helger
 * @see SMPRestCache
 * @since 8.4.4
 */
public class SMPRestCacheInvalidationCallback implements
                                             ISMPServiceGroupCallback,
                                             ISMPServiceInformationCallback,
                                             ISMPRedirectCallback
{
  private static void _invalidate (@NonNull final IParticipantIdentifier aParticipantID)
  {
    SMPRestCache.invalidateParticipant (aParticipantID);
  }

  public void onSMPServiceGroupCreated (@NonNull final ISMPServiceGroup aServiceGroup, final boolean bCreateInSML)
  {
    _invalidate (aServiceGroup.getParticipantIdentifier ());
  }

  public void onSMPServiceGroupUpdated (@NonNull final IParticipantIdentifier aParticipantID)
  {
    _invalidate (aParticipantID);
  }

  public void onSMPServiceGroupDeleted (@NonNull final IParticipantIdentifier aParticipantID,
                                        final boolean bDeleteInSML)
  {
    _invalidate (aParticipantID);
  }

  @Override
  public void onSMPServiceInformationCreated (@NonNull final ISMPServiceInformation aServiceInformation)
  {
    _invalidate (aServiceInformation.getServiceGroupParticipantIdentifier ());
  }

  @Override
  public void onSMPServiceInformationUpdated (@NonNull final ISMPServiceInformation aServiceInformation)
  {
    _invalidate (aServiceInformation.getServiceGroupParticipantIdentifier ());
  }

  @Override
  public void onSMPServiceInformationDeleted (@NonNull final ISMPServiceInformation aServiceInformation)
  {
    _invalidate (aServiceInformation.getServiceGroupParticipantIdentifier ());
  }

  @Override
  public void onSMPRedirectCreated (@NonNull final ISMPRedirect aRedirect)
  {
    _invalidate (aRedirect.getServiceGroupParticipantIdentifier ());
  }

  @Override
  public void onSMPRedirectUpdated (@NonNull final ISMPRedirect aRedirect)
  {
    _invalidate (aRedirect.getServiceGroupParticipantIdentifier ());
  }

  @Override
  public void onSMPRedirectDeleted (@NonNull final ISMPRedirect aRedirect)
  {
    _invalidate (aRedirect.getServiceGroupParticipantIdentifier ());
  }
}
