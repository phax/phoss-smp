/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupCallback;

/**
 * Special {@link ISMPServiceGroupCallback} to delete the business card, if the
 * service group is deleted.
 *
 * @author Philip Helger
 */
public class BusinessCardSMPServiceGroupCallback implements ISMPServiceGroupCallback
{
  private static final Logger LOGGER = LoggerFactory.getLogger (BusinessCardSMPServiceGroupCallback.class);

  private final ISMPBusinessCardManager m_aBusinessCardMgr;

  public BusinessCardSMPServiceGroupCallback (@Nonnull final ISMPBusinessCardManager aBusinessCardMgr)
  {
    ValueEnforcer.notNull (aBusinessCardMgr, "BusinessCardMgr");
    m_aBusinessCardMgr = aBusinessCardMgr;
  }

  public void onSMPServiceGroupCreated (@Nonnull final ISMPServiceGroup aServiceGroup)
  {}

  public void onSMPServiceGroupUpdated (@Nonnull final IParticipantIdentifier aParticipantID)
  {}

  @Override
  public void onSMPServiceGroupDeleted (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    // If service group is deleted, also delete respective business card
    final ISMPBusinessCard aBusinessCard = m_aBusinessCardMgr.getSMPBusinessCardOfID (aParticipantID);
    if (aBusinessCard != null)
      m_aBusinessCardMgr.deleteSMPBusinessCard (aBusinessCard);
    else
      if (LOGGER.isDebugEnabled ())
        LOGGER.warn ("Found no BusinessCard for participant ID '" + aParticipantID.getURIEncoded () + "'");
  }
}
