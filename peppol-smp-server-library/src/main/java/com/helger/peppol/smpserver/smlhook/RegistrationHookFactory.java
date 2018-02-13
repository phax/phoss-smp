/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.smlhook;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.peppol.smpserver.domain.SMPMetaManager;

/**
 * This class provides the {@link IRegistrationHook} instance that matches the
 * current "write to SML" settings.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Immutable
public final class RegistrationHookFactory
{
  private static RegistrationHookDoNothing s_aDoNothing = new RegistrationHookDoNothing ();
  private static RegistrationHookWriteToSML s_aWriteToSML = new RegistrationHookWriteToSML ();

  private RegistrationHookFactory ()
  {}

  /**
   * Get the one and only instance.
   *
   * @return A non-<code>null</code> instance of {@link IRegistrationHook}
   *         according to the current setting. This can be either an instance of
   *         {@link RegistrationHookDoNothing} or an instance of
   *         {@link RegistrationHookWriteToSML}.
   */
  @Nonnull
  public static IRegistrationHook getInstance ()
  {
    return SMPMetaManager.getSettings ().isSMLActive () ? s_aWriteToSML : s_aDoNothing;
  }
}
