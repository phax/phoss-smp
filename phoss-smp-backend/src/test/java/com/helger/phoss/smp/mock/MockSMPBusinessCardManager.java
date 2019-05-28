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
package com.helger.phoss.smp.mock;

import java.util.Collection;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardCallback;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardEntity;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;

/**
 * Mock implementation of {@link ISMPBusinessCardManager}.
 *
 * @author Philip Helger
 */
final class MockSMPBusinessCardManager implements ISMPBusinessCardManager
{
  @Nonnull
  @ReturnsMutableObject
  public CallbackList <ISMPBusinessCardCallback> bcCallbacks ()
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPBusinessCard getSMPBusinessCardOfServiceGroup (final ISMPServiceGroup aServiceGroup)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPBusinessCard getSMPBusinessCardOfID (final String sID)
  {
    throw new UnsupportedOperationException ();
  }

  public int getSMPBusinessCardCount ()
  {
    return 0;
  }

  public ICommonsList <ISMPBusinessCard> getAllSMPBusinessCards ()
  {
    throw new UnsupportedOperationException ();
  }

  public EChange deleteSMPBusinessCard (final ISMPBusinessCard aSMPBusinessCard)
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPBusinessCard createOrUpdateSMPBusinessCard (final ISMPServiceGroup aServiceGroup,
                                                         final Collection <SMPBusinessCardEntity> aEntities)
  {
    throw new UnsupportedOperationException ();
  }
}
