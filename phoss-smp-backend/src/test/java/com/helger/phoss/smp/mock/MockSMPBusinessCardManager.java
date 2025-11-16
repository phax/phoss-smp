/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
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

  @NonNull
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

  public boolean containsSMPBusinessCardOfID (@Nullable final IParticipantIdentifier aID)
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
