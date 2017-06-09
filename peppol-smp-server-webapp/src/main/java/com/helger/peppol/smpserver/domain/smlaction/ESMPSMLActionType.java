package com.helger.peppol.smpserver.domain.smlaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

public enum ESMPSMLActionType implements IHasID <String>
{
  CREATE ("create"),
  UPDATE ("update"),
  DELETE ("delete");

  private final String m_sID;

  private ESMPSMLActionType (@Nonnull @Nonempty final String sID)
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
  public static ESMPSMLActionType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESMPSMLActionType.class, sID);
  }
}
