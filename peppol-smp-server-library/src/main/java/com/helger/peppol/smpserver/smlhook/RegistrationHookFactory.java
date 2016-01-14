/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver.smlhook;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.peppol.smpserver.SMPServerConfiguration;

/**
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Immutable
public final class RegistrationHookFactory
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (RegistrationHookFactory.class);

  private static final ReadWriteLock s_aRWLock = new ReentrantReadWriteLock ();
  private static IRegistrationHook s_aInstance;

  private RegistrationHookFactory ()
  {}

  /**
   * Create a new instance every time this method is invoked.
   *
   * @param bWriteToSML
   *        <code>true</code> if writing to SML should be enabled,
   *        <code>false</code> otherwise.
   * @return A new instance of {@link IRegistrationHook} according to the
   *         configuration file.
   * @throws IllegalStateException
   *         If the class could not be instantiated
   */
  @Nonnull
  public static IRegistrationHook createInstance (final boolean bWriteToSML)
  {
    s_aLogger.info ("Access to the SML is " + (bWriteToSML ? "enabled" : "disabled") + " in this SMP server!");
    return bWriteToSML ? new RegistrationHookWriteToSML () : new RegistrationHookDoNothing ();
  }

  /**
   * Get the one and only instance.
   *
   * @return A new instance of {@link IRegistrationHook} according to the
   *         configuration file.
   * @throws IllegalStateException
   *         If the class could not be instantiated
   */
  @Nonnull
  public static IRegistrationHook getOrCreateInstance ()
  {
    IRegistrationHook ret;
    s_aRWLock.readLock ().lock ();
    try
    {
      ret = s_aInstance;
    }
    finally
    {
      s_aRWLock.readLock ().unlock ();
    }

    if (ret == null)
    {
      s_aRWLock.writeLock ().lock ();
      try
      {
        // Try again in write lock
        ret = s_aInstance;
        if (ret == null)
        {
          final boolean bWriteToSML = SMPServerConfiguration.isWriteToSML ();
          s_aInstance = ret = createInstance (bWriteToSML);
        }
      }
      finally
      {
        s_aRWLock.writeLock ().unlock ();
      }
    }
    return ret;
  }

  public static void setInstance (@Nonnull final IRegistrationHook aInstance)
  {
    ValueEnforcer.notNull (aInstance, "Instance");
    s_aRWLock.writeLock ().lock ();
    try
    {
      s_aInstance = aInstance;
    }
    finally
    {
      s_aRWLock.writeLock ().unlock ();
    }
  }

  public static void setInstance (final boolean bWriteToSML)
  {
    setInstance (createInstance (bWriteToSML));
  }

  public static boolean isSMLConnectionActive ()
  {
    return getOrCreateInstance () instanceof RegistrationHookWriteToSML;
  }
}
