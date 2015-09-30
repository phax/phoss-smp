package com.helger.peppol.smpserver.domain.user;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.peppol.smpserver.domain.user.ISMPUser;
import com.helger.photon.basic.security.user.IUser;

public class XMLDataUser implements ISMPUser
{
  private final IUser m_aUser;
  private final String m_sName;

  public XMLDataUser (@Nonnull final IUser aUser)
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
