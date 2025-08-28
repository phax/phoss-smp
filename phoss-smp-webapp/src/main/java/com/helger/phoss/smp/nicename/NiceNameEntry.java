/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.nicename;

import java.io.Serializable;

import com.helger.annotation.Nonempty;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.CollectionHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.peppol.EPeppolCodeListItemState;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public final class NiceNameEntry implements Serializable
{
  private final String m_sName;
  private final EPeppolCodeListItemState m_eState;
  private final String m_sSpecialLabel;
  private final ICommonsList <IProcessIdentifier> m_aProcIDs;

  public NiceNameEntry (@Nonnull @Nonempty final String sName,
                        @Nonnull final EPeppolCodeListItemState eState,
                        @Nullable final String sSpecialLabel,
                        @Nullable final ICommonsList <IProcessIdentifier> aProcIDs)
  {
    m_sName = sName;
    m_eState = eState;
    m_sSpecialLabel = sSpecialLabel;
    m_aProcIDs = aProcIDs;
  }

  @Nonnull
  @Nonempty
  public String getName ()
  {
    return m_sName;
  }

  @Nonnull
  public EPeppolCodeListItemState getState ()
  {
    return m_eState;
  }

  @Nullable
  public String getSpecialLabel ()
  {
    return m_sSpecialLabel;
  }

  public boolean hasProcessIDs ()
  {
    return CollectionHelper.isNotEmpty (m_aProcIDs);
  }

  public boolean containsProcessID (@Nonnull final IProcessIdentifier aProcID)
  {
    return m_aProcIDs != null && m_aProcIDs.containsAny (aProcID::hasSameContent);
  }

  @Nullable
  public ICommonsList <IProcessIdentifier> getAllProcIDs ()
  {
    return m_aProcIDs == null ? null : m_aProcIDs.getClone ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final NiceNameEntry rhs = (NiceNameEntry) o;
    return m_sName.equals (rhs.m_sName) && m_eState == rhs.m_eState;
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sName).append (m_eState).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("Name", m_sName)
                                       .append ("State", m_eState)
                                       .appendIfNotNull ("SpecialLabel", m_sSpecialLabel)
                                       .append ("ProcessIDs", m_aProcIDs)
                                       .getToString ();
  }
}
