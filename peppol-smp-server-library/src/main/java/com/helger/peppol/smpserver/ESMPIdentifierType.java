package com.helger.peppol.smpserver;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * Defines the identifier types to be used - PEPPOL or simple.
 * 
 * @author Philip Helger
 */
public enum ESMPIdentifierType implements IHasID <String>
{
  SIMPLE ("simple"),
  PEPPOL ("peppol");

  private final String m_sID;

  private ESMPIdentifierType (@Nonnull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  @Nullable
  public static ESMPIdentifierType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESMPIdentifierType.class, sID);
  }

  @Nullable
  public static ESMPIdentifierType getFromIDOrDefault (@Nullable final String sID,
                                                       @Nullable final ESMPIdentifierType eDefault)
  {
    return EnumHelper.getFromIDOrDefault (ESMPIdentifierType.class, sID, eDefault);
  }
}
