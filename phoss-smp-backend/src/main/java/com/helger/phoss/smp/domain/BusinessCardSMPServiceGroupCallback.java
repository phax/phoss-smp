/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupCallback;
import com.helger.phoss.smp.domain.servicegroup.SMPServiceGroup;

/**
 * Special {@link ISMPServiceGroupCallback} to delete the business card, if the
 * service group is deleted.
 *
 * @author Philip Helger
 */
public class BusinessCardSMPServiceGroupCallback implements ISMPServiceGroupCallback
{
  private final ISMPBusinessCardManager m_aBusinessCardMgr;

  public BusinessCardSMPServiceGroupCallback (@Nonnull final ISMPBusinessCardManager aBusinessCardMgr)
  {
    ValueEnforcer.notNull (aBusinessCardMgr, "BusinessCardMgr");
    m_aBusinessCardMgr = aBusinessCardMgr;
  }

  @Override
  public void onSMPServiceGroupDeleted (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    // If service group is deleted, also delete respective business card
    final String sServiceGroupID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
    final ISMPBusinessCard aBusinessCard = m_aBusinessCardMgr.getSMPBusinessCardOfID (sServiceGroupID);
    if (aBusinessCard != null)
      m_aBusinessCardMgr.deleteSMPBusinessCard (aBusinessCard);
  }
}
