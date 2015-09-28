package com.helger.peppol.smpserver.data;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;

/**
 * Abstract interface representing a user.
 *
 * @author Philip Helger
 */
public interface IDataUser
{
  @Nonnull
  @Nonempty
  String getUserName ();
}
