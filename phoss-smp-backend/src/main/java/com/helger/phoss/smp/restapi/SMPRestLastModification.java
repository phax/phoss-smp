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
package com.helger.phoss.smp.restapi;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.phoss.smp.domain.servicegroup.SMPServiceGroup;

/**
 * Determine the last modification of a REST API response, as used for the HTTP header
 * <code>Last-Modified</code> (OASIS SMP 2.0, section 5.2.2).<br>
 * The effective last modification is the maximum of the last modification of the participant
 * (stored in the backend) and a global last modification of this SMP instance. The global last
 * modification covers all changes that affect every response, but that are not stored per
 * participant: it is set when this class is loaded (so after the configuration was read) and
 * updated when the signing key is reloaded.
 *
 * @author Philip Helger
 * @since 8.9.0
 */
@ThreadSafe
public final class SMPRestLastModification
{
  private static final AtomicReference <LocalDateTime> GLOBAL_LAST_MOD_DT = new AtomicReference <> (SMPServiceGroup.getCurrentLastModificationDateTime ());

  private SMPRestLastModification ()
  {}

  /**
   * @return The global last modification date time of this SMP instance. Never <code>null</code>.
   */
  @NonNull
  public static LocalDateTime getGlobalLastModificationDateTime ()
  {
    return GLOBAL_LAST_MOD_DT.get ();
  }

  /**
   * Set the global last modification date time of this SMP instance to now. This must be called for
   * every change that affects all REST API responses, like a reload of the signing key.
   */
  public static void updateGlobalLastModificationDateTime ()
  {
    GLOBAL_LAST_MOD_DT.set (SMPServiceGroup.getCurrentLastModificationDateTime ());
  }

  /**
   * Get the effective last modification date time of a REST API response.
   *
   * @param aParticipantLastModDT
   *        The last modification date time of the participant. May be <code>null</code> if it is
   *        unknown, because the participant was not modified since the backend maintains it.
   * @return The later one of the provided date time and the global last modification date time.
   *         Never <code>null</code>.
   */
  @NonNull
  public static LocalDateTime getEffectiveLastModificationDateTime (@Nullable final LocalDateTime aParticipantLastModDT)
  {
    final LocalDateTime aGlobalLastModDT = getGlobalLastModificationDateTime ();
    if (aParticipantLastModDT == null || aParticipantLastModDT.isBefore (aGlobalLastModDT))
      return aGlobalLastModDT;
    return aParticipantLastModDT;
  }
}
