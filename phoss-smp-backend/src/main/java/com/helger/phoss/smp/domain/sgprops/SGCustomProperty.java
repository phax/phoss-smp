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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.name.IHasName;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.cache.regex.RegExHelper;
import com.helger.json.IHasJson;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;

/**
 * Defines a single Service Group custom property.
 * 
 * @author Philip Helger
 * @since 8.1.0
 */
@Immutable
public final class SGCustomProperty implements IHasName, IHasJson
{
  public static final int NAME_MAX_LEN = 256;
  public static final int VALUE_MAX_LEN = 256;

  private @NonNull final ESGCustomPropertyType m_eType;
  private @NonNull @Nonempty final String m_sName;
  private @NonNull @Nonempty final String m_sValue;

  public static boolean isValidName (@Nullable final String s)
  {
    if (s == null)
      return false;
    final int nLen = s.length ();
    if (nLen == 0 || nLen > NAME_MAX_LEN)
      return false;
    return RegExHelper.stringMatchesPattern ("[a-zA-Z0-9\\.\\-_]{1," + NAME_MAX_LEN + "}", s);
  }

  public static boolean isValidValue (@Nullable final String s)
  {
    if (s == null)
      return false;
    final int nLen = s.length ();
    if (nLen > VALUE_MAX_LEN)
      return false;
    // No further restrictions
    // Empty value is okay
    return true;
  }

  public SGCustomProperty (@NonNull final ESGCustomPropertyType eType,
                           @NonNull @Nonempty final String sName,
                           @NonNull final String sValue)
  {
    ValueEnforcer.notNull (eType, "Type");
    ValueEnforcer.isTrue (isValidName (sName), () -> "Name '" + sName + "' is invalid");
    ValueEnforcer.isTrue (isValidValue (sValue), () -> "Value '" + sValue + "' is invalid");
    m_eType = eType;
    m_sName = sName;
    m_sValue = sValue;
  }

  @NonNull
  public ESGCustomPropertyType getType ()
  {
    return m_eType;
  }

  public boolean isPrivate ()
  {
    return m_eType.isPrivate ();
  }

  public boolean isPublic ()
  {
    return m_eType.isPublic ();
  }

  @NonNull
  @Nonempty
  public String getName ()
  {
    return m_sName;
  }

  @NonNull
  public String getValue ()
  {
    return m_sValue;
  }

  public boolean hasValue ()
  {
    return !m_sValue.isEmpty ();
  }

  @NonNull
  @ReturnsMutableObject
  public IJsonObject getAsJson ()
  {
    return new JsonObject ().add ("type", m_eType.getID ()).add ("name", m_sName).add ("value", m_sValue);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (!getClass ().equals (o.getClass ()))
      return false;
    final SGCustomProperty rhs = (SGCustomProperty) o;
    return m_sName.equals (rhs.m_sName);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sName).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Type", m_eType)
                                       .append ("Name", m_sName)
                                       .append ("Value", m_sValue)
                                       .getToString ();
  }

  @NonNull
  public static SGCustomProperty createPrivate (@NonNull @Nonempty final String sName, @NonNull final String sValue)
  {
    return new SGCustomProperty (ESGCustomPropertyType.PRIVATE, sName, sValue);
  }

  @NonNull
  public static SGCustomProperty createPublic (@NonNull @Nonempty final String sName, @NonNull final String sValue)
  {
    return new SGCustomProperty (ESGCustomPropertyType.PUBLIC, sName, sValue);
  }

  @NonNull
  public static SGCustomProperty fromJson (@NonNull final IJsonObject aJson)
  {
    final String sType = aJson.getAsString ("type");
    final ESGCustomPropertyType eType = ESGCustomPropertyType.getFromIDOrNull (sType);
    if (eType == null)
      throw new IllegalStateException ("Failed to resolve SG custom property type '" + sType + "'");
    return new SGCustomProperty (eType, aJson.getAsString ("name"), aJson.getAsString ("value"));
  }
}
