package com.helger.peppol.smpserver.domain.user;

import javax.annotation.Nonnull;

/**
 * Abstract interface representing a user.
 *
 * @author Philip Helger
 */
public interface ISMPUserEditable extends ISMPUser
{
  void setUserName (@Nonnull String sUserName);

  @Nonnull
  String getPassword ();

  void setPassword (@Nonnull String sPassword);
}
