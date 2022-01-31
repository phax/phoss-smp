/*
 * Copyright (C) 2015-2022 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.mock;

import java.util.Collection;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.state.EChange;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardCallback;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardEntity;

/**
 * Mock implementation of {@link ISMPBusinessCardManager}.
 *
 * @author Philip Helger
 */
final class MockSMPBusinessCardManager implements ISMPBusinessCardManager
{
  private final CallbackList <ISMPBusinessCardCallback> m_aCBs = new CallbackList <> ();

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <ISMPBusinessCardCallback> bcCallbacks ()
  {
    return m_aCBs;
  }

  public ISMPBusinessCard createOrUpdateSMPBusinessCard (final IParticipantIdentifier aParticipantID,
                                                         final Collection <SMPBusinessCardEntity> aEntities)
  {
    throw new UnsupportedOperationException ();
  }

  public EChange deleteSMPBusinessCard (final ISMPBusinessCard aSMPBusinessCard)
  {
    throw new UnsupportedOperationException ();
  }

  public ICommonsList <ISMPBusinessCard> getAllSMPBusinessCards ()
  {
    throw new UnsupportedOperationException ();
  }

  public ICommonsSet <String> getAllSMPBusinessCardIDs ()
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPBusinessCard getSMPBusinessCardOfID (final IParticipantIdentifier aID)
  {
    throw new UnsupportedOperationException ();
  }

  public long getSMPBusinessCardCount ()
  {
    return 0;
  }
}
