/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.mock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpserver.domain.transportprofile.ISMPTransportProfileManager;

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

  @Nonnull
  public EChange updateSMPTransportProfile (final String sSMPTransportProfileID,
                                            final String sName,
                                            final boolean bIsDeprecated)
  {
    throw new UnsupportedOperationException ();
  }

  @Nonnull
  public EChange removeSMPTransportProfile (final String sSMPTransportProfileID)
  {
    throw new UnsupportedOperationException ();
  }

  @Nullable
  public ISMPTransportProfile getSMPTransportProfileOfID (final String sID)
  {
    return null;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPTransportProfile> getAllSMPTransportProfiles ()
  {
    throw new UnsupportedOperationException ();
  }

  public boolean containsSMPTransportProfileWithID (final String sID)
  {
    return false;
  }
}
