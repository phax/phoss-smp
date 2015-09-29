package com.helger.peppol.smpserver.data;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;

/**
 * Abstract interface representing a user.
 *
 * @author Philip Helger
 */
public interface IDataUser extends IHasID <String>
{
  @Nonnull
  @Nonempty
  String getID ();

  @Nonnull
  @Nonempty
  String getUserName ();
}
