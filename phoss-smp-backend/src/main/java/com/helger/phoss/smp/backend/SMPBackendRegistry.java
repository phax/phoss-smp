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
package com.helger.phoss.smp.backend;

import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.spi.ServiceLoaderHelper;
import com.helger.base.string.StringHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsMap;
import com.helger.collection.commons.ICommonsSet;
import com.helger.phoss.smp.domain.ISMPManagerProvider;

/**
 * This class contains all registered SMP backends with an ID and an {@link ISMPManagerProvider}
 * factory to be used. The registration of the respective backends happens via the SPI interface
 * {@link ISMPBackendRegistrarSPI}.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class SMPBackendRegistry implements ISMPBackendRegistry
{
  private static final class SingletonHolder
  {
    static final SMPBackendRegistry INSTANCE = new SMPBackendRegistry ();
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPBackendRegistry.class);
  private static boolean s_bDefaultInstantiated = false;

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  private final ICommonsMap <String, Supplier <? extends ISMPManagerProvider>> m_aMap = new CommonsLinkedHashMap <> ();

  private SMPBackendRegistry ()
  {
    reinitialize ();
  }

  public static boolean isInstantiated ()
  {
    return s_bDefaultInstantiated;
  }

  /**
   * @return The singleton instance of this class. Never <code>null</code>.
   */
  @NonNull
  public static SMPBackendRegistry getInstance ()
  {
    final SMPBackendRegistry ret = SingletonHolder.INSTANCE;
    s_bDefaultInstantiated = true;
    return ret;
  }

  public void registerSMPBackend (@NonNull @Nonempty final String sID,
                                  @NonNull final Supplier <? extends ISMPManagerProvider> aFactory)
  {
    ValueEnforcer.notEmpty (sID, "ID");
    ValueEnforcer.notNull (aFactory, "Factory");

    m_aRWLock.writeLocked ( () -> {
      if (m_aMap.containsKey (sID))
        throw new IllegalArgumentException ("Another SMP backend with ID '" + sID + "' is already registered!");
      m_aMap.put (sID, aFactory);
    });
  }

  /**
   * Find and instantiate the {@link ISMPManagerProvider} for the provided backend ID.
   *
   * @param sBackendID
   *        The backend ID to be searched. May be <code>null</code> or empty.
   * @return <code>null</code> if no manager provider for the provided ID is present or if the
   *         factory created a <code>null</code> instance.
   */
  @Nullable
  public ISMPManagerProvider getManagerProvider (@Nullable final String sBackendID)
  {
    if (StringHelper.isEmpty (sBackendID))
      return null;

    final Supplier <? extends ISMPManagerProvider> aFactory = m_aRWLock.readLockedGet ( () -> m_aMap.get (sBackendID));
    return aFactory == null ? null : aFactory.get ();
  }

  /**
   * @return A set with all registered backend IDs. Never <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllBackendIDs ()
  {
    return m_aRWLock.readLockedGet (m_aMap::copyOfKeySet);
  }

  /**
   * Remove all registered backends and re-read the information from the SPI providers.
   */
  public void reinitialize ()
  {
    m_aRWLock.writeLocked ( () -> {
      m_aMap.clear ();

      // register all SPI implementations
      for (final ISMPBackendRegistrarSPI aSPI : ServiceLoaderHelper.getAllSPIImplementations (ISMPBackendRegistrarSPI.class))
      {
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Calling registerSMPBackend on " + aSPI.getClass ().getName ());
        aSPI.registerSMPBackend (this);
      }

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (m_aMap.size () + " SMP backends registered");
    });
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("map", m_aMap).getToString ();
  }
}
