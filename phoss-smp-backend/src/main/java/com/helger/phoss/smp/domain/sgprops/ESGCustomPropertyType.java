/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.sgprops;

import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.base.lang.EnumHelper;
import com.helger.text.display.IHasDisplayText;

/**
 * Defines the types for ServiceGroup Custom Properties.
 * 
 * @author Philip Helger
 * @since 8.1.0
 */
public enum ESGCustomPropertyType implements IHasID <String>, IHasDisplayText
{
  PUBLIC ("pub", ESGCustomPropertyTypeName.PUBLIC),
  PRIVATE ("priv", ESGCustomPropertyTypeName.PRIVATE);

  /** The default property type is: public */
  public static final ESGCustomPropertyType DEFAULT = PUBLIC;

  // for DB storage
  public static final int ID_MAX_LEN = 6;

  private final String m_sID;
  private final ESGCustomPropertyTypeName m_eName;

  private ESGCustomPropertyType (@NonNull @Nonempty final String sID, @NonNull final ESGCustomPropertyTypeName eName)
  {
    m_sID = sID;
    m_eName = eName;
  }

  @NonNull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  public boolean isPrivate ()
  {
    return this == PRIVATE;
  }

  public boolean isPublic ()
  {
    return this == PUBLIC;
  }

  @Nullable
  public String getDisplayText (@NonNull final Locale aContentLocale)
  {
    return m_eName.getDisplayText (aContentLocale);
  }

  @Nullable
  public static ESGCustomPropertyType getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (ESGCustomPropertyType.class, sID);
  }
}
