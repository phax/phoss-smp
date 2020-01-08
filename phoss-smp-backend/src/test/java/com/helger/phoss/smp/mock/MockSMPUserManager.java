/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.mock;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.user.ISMPUser;
import com.helger.phoss.smp.domain.user.ISMPUserManager;

/**
 * Mock implementation of {@link ISMPUserManager}.
 *
 * @author Philip Helger
 */
final class MockSMPUserManager implements ISMPUserManager
{
  public boolean isSpecialUserManagementNeeded ()
  {
    return false;
  }

  @Nonnull
  public ESuccess createUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    throw new UnsupportedOperationException ();
  }

  @Nonnull
  public ESuccess updateUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    throw new UnsupportedOperationException ();
  }

  @Nonnull
  public EChange deleteUser (@Nullable final String sUserName)
  {
    throw new UnsupportedOperationException ();
  }

  @Nullable
  public ISMPUser getUserOfID (@Nullable final String sUserID)
  {
    throw new UnsupportedOperationException ();
  }

  @Nonnegative
  public int getUserCount ()
  {
    return 0;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPUser> getAllUsers ()
  {
    throw new UnsupportedOperationException ();
  }

  @Nonnull
  public ISMPUser validateUserCredentials (final BasicAuthClientCredentials aCredentials)
  {
    throw new UnsupportedOperationException ();
  }

  @Nullable
  public Object verifyOwnership (final IParticipantIdentifier aServiceGroupID, final ISMPUser aCurrentUser)
  {
    throw new UnsupportedOperationException ();
  }
}
