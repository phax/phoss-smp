/*
 * Copyright (C) 2015-2023 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.businesscard;

import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.locale.LocaleHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.peppol.businesscard.generic.PDName;
import com.helger.peppol.businesscard.v3.PD3APIHelper;
import com.helger.peppol.businesscard.v3.PD3MultilingualNameType;

/**
 * Generic name.
 *
 * @author Philip Helger
 */
@Immutable
public class SMPBusinessCardName implements Serializable
{
  public static final String JSON_TAG_NAME = "name";
  public static final String JSON_TAG_LANGUAGE = "language";

  private final String m_sName;
  private final String m_sLanguageCode;

  public SMPBusinessCardName (@Nonnull @Nonempty final String sName, @Nullable final String sLanguageCode)
  {
    ValueEnforcer.notEmpty (sName, "Name");
    ValueEnforcer.isTrue (PDName.isValidLanguageCode (sLanguageCode),
                          () -> "'" + sLanguageCode + "' is invalid language code");
    m_sName = sName;
    m_sLanguageCode = LocaleHelper.getValidLanguageCode (sLanguageCode);
  }

  /**
   * @return The name. May be <code>null</code>.
   */
  @Nonnull
  @Nonempty
  public String getName ()
  {
    return m_sName;
  }

  /**
   * @return The language code. May be <code>null</code>.
   */
  @Nullable
  public String getLanguageCode ()
  {
    return m_sLanguageCode;
  }

  public boolean hasLanguageCode ()
  {
    return StringHelper.hasText (m_sLanguageCode);
  }

  public boolean hasNoLanguageCode ()
  {
    return StringHelper.hasNoText (m_sLanguageCode);
  }

  @Nonnull
  public PD3MultilingualNameType getAsJAXBObject ()
  {
    return PD3APIHelper.createName (m_sName, m_sLanguageCode);
  }

  @Nonnull
  public IJsonObject getAsJson ()
  {
    final IJsonObject ret = new JsonObject ().add (JSON_TAG_NAME, m_sName);
    if (StringHelper.hasText (m_sLanguageCode))
      ret.add (JSON_TAG_LANGUAGE, m_sLanguageCode);
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;

    final SMPBusinessCardName rhs = (SMPBusinessCardName) o;
    return m_sName.equals (rhs.m_sName) && EqualsHelper.equals (m_sLanguageCode, rhs.m_sLanguageCode);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sName).append (m_sLanguageCode).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Name", m_sName)
                                       .appendIfNotNull ("LanguageCode", m_sLanguageCode)
                                       .getToString ();
  }

  @Nullable
  public static SMPBusinessCardName createFromJson (@Nonnull final IJsonObject aJson)
  {
    try
    {
      return new SMPBusinessCardName (aJson.getAsString (JSON_TAG_NAME), aJson.getAsString (JSON_TAG_LANGUAGE));
    }
    catch (final RuntimeException ex)
    {
      return null;
    }
  }
}
