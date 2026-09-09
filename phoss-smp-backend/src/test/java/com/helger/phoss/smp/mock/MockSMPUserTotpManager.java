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
package com.helger.phoss.smp.mock;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.state.EChange;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.domain.totp.ISMPUserTotp;
import com.helger.phoss.smp.domain.totp.ISMPUserTotpManager;

/**
 * Mock implementation of {@link ISMPUserTotpManager}.
 *
 * @author Philip Helger
 * @since 8.4.3
 */
final class MockSMPUserTotpManager implements ISMPUserTotpManager
{
  @NonNull
  public ISMPUserTotp createOrReplaceTotp (@NonNull @Nonempty final String sUserID,
                                           @NonNull @Nonempty final String sSecret)
  {
    throw new UnsupportedOperationException ();
  }

  @NonNull
  public EChange setTotpEnabled (@Nullable final String sUserID, final boolean bEnabled)
  {
    return EChange.UNCHANGED;
  }

  @NonNull
  public EChange setTotpLastUsedTimeSlot (@Nullable final String sUserID, final long nTimeSlot)
  {
    return EChange.UNCHANGED;
  }

  @NonNull
  public EChange deleteTotp (@Nullable final String sUserID)
  {
    return EChange.UNCHANGED;
  }

  @Nullable
  public ISMPUserTotp getTotpOfUserID (@Nullable final String sUserID)
  {
    return null;
  }

  @NonNull
  public ICommonsList <ISMPUserTotp> getAllTotps ()
  {
    return new CommonsArrayList <> ();
  }
}
