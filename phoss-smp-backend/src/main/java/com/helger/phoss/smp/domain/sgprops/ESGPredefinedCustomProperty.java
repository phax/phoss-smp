/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
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
import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.lang.EnumHelper;
import com.helger.base.name.IHasName;
import com.helger.cache.regex.RegExHelper;
import com.helger.text.display.IHasDisplayText;

/**
 * Defines predefined custom property names for ServiceGroups with well-known meaning.
 *
 * @author Philip Helger
 * @since 8.1.0
 */
public enum ESGPredefinedCustomProperty implements IHasName, IHasDisplayText
{
  /** Croatian OIB number */
  HR_OIB ("hr.oib",
          ESGPredefinedCustomPropertyText.HR_OIB,
          value -> RegExHelper.stringMatchesPattern ("\\d{11}", value),
          ESGPredefinedCustomPropertyText.HR_OIB_RULE);

  private final String m_sName;
  private final ESGPredefinedCustomPropertyText m_eName;
  private final Predicate <String> m_aValueChecker;
  private final ESGPredefinedCustomPropertyText m_eValueRule;

  private ESGPredefinedCustomProperty (@NonNull @Nonempty final String sID,
                                       @NonNull final ESGPredefinedCustomPropertyText eName,
                                       @NonNull final Predicate <String> aValueChecker,
                                       @NonNull final ESGPredefinedCustomPropertyText eValueRule)
  {
    m_sName = sID;
    m_eName = eName;
    m_aValueChecker = aValueChecker;
    m_eValueRule = eValueRule;
  }

  @NonNull
  @Nonempty
  public String getName ()
  {
    return m_sName;
  }

  @Nullable
  public String getDisplayText (@NonNull final Locale aContentLocale)
  {
    return m_eName.getDisplayText (aContentLocale);
  }

  /**
   * Check if the value is valid according to the special rules of this custom property.
   * 
   * @param sValue
   *        The value to check, may be <code>null</code>.
   * @return <code>true</code> if the value is valid, <code>false</code> if not.
   */
  public boolean isValueValid (@Nullable final String sValue)
  {
    // General value check
    if (!SGCustomProperty.isValidValue (sValue))
      return false;

    // No specific value checker -> valid
    if (m_aValueChecker == null)
      return true;
    return m_aValueChecker.test (sValue);
  }

  @Nullable
  public String getValueRuleDisplayText (@NonNull final Locale aContentLocale)
  {
    return m_eValueRule.getDisplayText (aContentLocale);
  }

  @Nullable
  public static ESGPredefinedCustomProperty getFromNameOrNull (@Nullable final String sName)
  {
    return EnumHelper.getFromNameOrNull (ESGPredefinedCustomProperty.class, sName);
  }

  public static boolean isPredefined (@Nullable final String sName)
  {
    return getFromNameOrNull (sName) != null;
  }
}
