package com.helger.peppol.smpserver.domain.user;

import java.io.Serializable;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;

/**
 * Abstract interface representing a user.
 *
 * @author Philip Helger
 */
public interface ISMPUser extends IHasID <String>, Serializable
{
  @Nonnull
  @Nonempty
  String getID ();

  @Nonnull
  @Nonempty
  String getUserName ();
}
