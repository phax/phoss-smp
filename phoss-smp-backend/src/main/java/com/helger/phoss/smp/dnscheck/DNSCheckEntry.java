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
package com.helger.phoss.smp.dnscheck;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.peppolid.IParticipantIdentifier;

/**
 * The DNS check outcome of a single participant.
 *
 * @author Philip Helger
 * @since 8.5.1
 */
@Immutable
public final class DNSCheckEntry
{
  private final IParticipantIdentifier m_aParticipantID;
  private final EDNSCheckState m_eState;
  private final String m_sDNSName;
  private final String m_sSMPURI;
  private final String m_sErrorMessage;

  public DNSCheckEntry (@NonNull final IParticipantIdentifier aParticipantID,
                        @NonNull final EDNSCheckState eState,
                        @Nullable final String sDNSName,
                        @Nullable final String sSMPURI,
                        @Nullable final String sErrorMessage)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (eState, "State");
    m_aParticipantID = aParticipantID;
    m_eState = eState;
    m_sDNSName = sDNSName;
    m_sSMPURI = sSMPURI;
    m_sErrorMessage = sErrorMessage;
  }

  @NonNull
  public IParticipantIdentifier getParticipantIdentifier ()
  {
    return m_aParticipantID;
  }

  @NonNull
  public EDNSCheckState getState ()
  {
    return m_eState;
  }

  /**
   * @return The DNS name of the participant, or <code>null</code> if it could not even be built.
   */
  @Nullable
  public String getDNSName ()
  {
    return m_sDNSName;
  }

  /**
   * @return The SMP URI the participant resolves to, or <code>null</code> if it does not resolve.
   */
  @Nullable
  public String getSMPURI ()
  {
    return m_sSMPURI;
  }

  @Nullable
  public String getErrorMessage ()
  {
    return m_sErrorMessage;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("ParticipantID", m_aParticipantID)
                                       .append ("State", m_eState)
                                       .appendIfNotNull ("DNSName", m_sDNSName)
                                       .appendIfNotNull ("SMPURI", m_sSMPURI)
                                       .appendIfNotNull ("ErrorMessage", m_sErrorMessage)
                                       .getToString ();
  }
}
