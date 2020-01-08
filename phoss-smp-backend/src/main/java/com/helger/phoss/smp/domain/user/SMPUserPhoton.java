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
package com.helger.phoss.smp.domain.user;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.photon.security.user.IUser;

/**
 * An implementation of {@link ISMPUser} on top of the ph-oton {@link IUser}.
 * 
 * @author Philip Helger
 */
public class SMPUserPhoton implements ISMPUser
{
  private final IUser m_aUser;
  private final String m_sName;

  public SMPUserPhoton (@Nonnull final IUser aUser)
  {
    m_aUser = ValueEnforcer.notNull (aUser, "User");
    m_sName = aUser.getLoginName () + " (" + aUser.getDisplayName () + ")";
  }

  @Nonnull
  public IUser getUser ()
  {
    return m_aUser;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_aUser.getID ();
  }

  @Nonnull
  @Nonempty
  public String getUserName ()
  {
    return m_sName;
  }
}
