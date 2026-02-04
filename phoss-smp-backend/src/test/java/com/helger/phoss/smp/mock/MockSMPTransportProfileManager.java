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

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;

/**
 * Mock implementation of {@link ISMPTransportProfileManager}.
 *
 * @author Philip Helger
 */
final class MockSMPTransportProfileManager implements ISMPTransportProfileManager
{
  @Nullable
  public ISMPTransportProfile createSMPTransportProfile (final String sID,
                                                         final String sName,
                                                         final boolean bIsDeprecated)
  {
    throw new UnsupportedOperationException ();
  }

  @NonNull
  public EChange updateSMPTransportProfile (final String sSMPTransportProfileID,
                                            final String sName,
                                            final boolean bIsDeprecated)
  {
    throw new UnsupportedOperationException ();
  }

  @NonNull
  public EChange deleteSMPTransportProfile (final String sSMPTransportProfileID)
  {
    throw new UnsupportedOperationException ();
  }

  @Nullable
  public ISMPTransportProfile getSMPTransportProfileOfID (final String sID)
  {
    return null;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPTransportProfile> getAllSMPTransportProfiles ()
  {
    throw new UnsupportedOperationException ();
  }

  public boolean containsSMPTransportProfileWithID (final String sID)
  {
    return false;
  }

  @Nonnegative
  public long getSMPTransportProfileCount ()
  {
    return 0;
  }
}
