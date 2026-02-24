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
package com.helger.phoss.smp.domain.businesscard;

import java.io.Serializable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.equals.EqualsHelper;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.peppol.businesscard.generic.PDName;
import com.helger.peppol.businesscard.v3.PD3APIHelper;
import com.helger.peppol.businesscard.v3.PD3MultilingualNameType;
import com.helger.text.locale.LocaleHelper;

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

  public SMPBusinessCardName (@NonNull @Nonempty final String sName, @Nullable final String sLanguageCode)
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
  @NonNull
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
    return StringHelper.isNotEmpty (m_sLanguageCode);
  }

  public boolean hasNoLanguageCode ()
  {
    return StringHelper.isEmpty (m_sLanguageCode);
  }

  @NonNull
  public PD3MultilingualNameType getAsJAXBObject ()
  {
    return PD3APIHelper.createName (m_sName, m_sLanguageCode);
  }

  @NonNull
  public IJsonObject getAsJson ()
  {
    final IJsonObject ret = new JsonObject ().add (JSON_TAG_NAME, m_sName);
    if (StringHelper.isNotEmpty (m_sLanguageCode))
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
  public static SMPBusinessCardName createFromJson (@NonNull final IJsonObject aJson)
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
