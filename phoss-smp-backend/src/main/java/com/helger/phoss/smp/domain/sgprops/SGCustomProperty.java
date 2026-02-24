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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
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
@MustImplementEqualsAndHashcode
public final class SGCustomProperty implements IHasName, IHasJson
{
  public static final int NAME_MAX_LEN = 256;
  public static final int VALUE_MAX_LEN = 256;

  private final ESGCustomPropertyType m_eType;
  private final String m_sName;
  private final String m_sValue;

  /**
   * Check if the provided property name is valid or not. Valid names have a length between 1 and
   * 256 and must only contain alpha numeric characters, dot, minus or underscore.
   * 
   * @param s
   *        The String to check
   * @return <code>true</code> if the name is valid, <code>false</code> if not.
   * @see #isValidValue(String)
   */
  public static boolean isValidName (@Nullable final String s)
  {
    if (s == null)
      return false;
    final int nLen = s.length ();
    if (nLen == 0 || nLen > NAME_MAX_LEN)
      return false;
    return RegExHelper.stringMatchesPattern ("[a-zA-Z0-9\\.\\-_]{1," + NAME_MAX_LEN + "}", s);
  }

  public static boolean isCharacterForbiddenInValue (final char c)
  {
    return c == '\r' || c == '\n' || c == '\0';
  }

  /**
   * Check if the provided property value is valid or not. Valid values have a length between 0 and
   * 256 and must not contain newline characters ({@code \r}, {@code \n}) or the null character
   * ({@code \0}), because these cannot be properly represented in XML attributes.
   *
   * @param s
   *        The String to check
   * @return <code>true</code> if the value is valid, <code>false</code> if not.
   * @see #isValidName(String)
   */
  public static boolean isValidValue (@Nullable final String s)
  {
    if (s == null)
      return false;
    final int nLen = s.length ();
    if (nLen > VALUE_MAX_LEN)
      return false;

    // Disallow characters that cannot be properly represented in XML attributes
    for (final char c : s.toCharArray ())
      if (isCharacterForbiddenInValue (c))
        return false;

    // Empty value is okay
    return true;
  }

  public SGCustomProperty (@NonNull final ESGCustomPropertyType eType,
                           @NonNull @Nonempty final String sName,
                           @NonNull final String sValue)
  {
    ValueEnforcer.notNull (eType, "Type");
    if (!isValidName (sName))
      throw new IllegalArgumentException ("Name '" + sName + "' is invalid");
    if (!isValidValue (sValue))
      throw new IllegalArgumentException ("Value '" + sValue + "' is invalid");
    m_eType = eType;
    m_sName = sName;
    m_sValue = sValue;
  }

  /**
   * @return The custom property type. Never <code>null</code>.
   */
  @NonNull
  public ESGCustomPropertyType getType ()
  {
    return m_eType;
  }

  /**
   * @return <code>true</code> if this is a private property.
   */
  public boolean isPrivate ()
  {
    return m_eType.isPrivate ();
  }

  /**
   * @return <code>true</code> if this is a public property.
   */
  public boolean isPublic ()
  {
    return m_eType.isPublic ();
  }

  /**
   * Get the custom property name. Is case-sensitive.
   */
  @NonNull
  @Nonempty
  public String getName ()
  {
    return m_sName;
  }

  /**
   * @return <code>true</code> if this custom property has a name, <code>false</code> if not.
   */
  public boolean hasValue ()
  {
    return !m_sValue.isEmpty ();
  }

  /**
   * @return The custom property value. May be empty but never <code>null</code>.
   */
  @NonNull
  public String getValue ()
  {
    return m_sValue;
  }

  /**
   * Get a simple JSON representation of this object.
   */
  @NonNull
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
    return m_eType.equals (rhs.m_eType) && m_sName.equals (rhs.m_sName) && m_sValue.equals (rhs.m_sValue);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_eType).append (m_sName).append (m_sValue).getHashCode ();
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
