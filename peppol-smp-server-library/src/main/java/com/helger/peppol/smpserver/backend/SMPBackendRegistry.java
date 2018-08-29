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
package com.helger.peppol.smpserver.backend;

import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.functional.ISupplier;
import com.helger.commons.lang.ServiceLoaderHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.peppol.smpserver.domain.ISMPManagerProvider;

/**
 * This class contains all registered SMP backends with an ID and an
 * {@link ISMPManagerProvider} factory to be used. The registration of the
 * respective backends happens via the SPI interface
 * {@link ISMPBackendRegistrarSPI}.
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class SMPBackendRegistry implements ISMPBackendRegistry
{
  private static final class SingletonHolder
  {
    static final SMPBackendRegistry s_aInstance = new SMPBackendRegistry ();
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPBackendRegistry.class);
  private static boolean s_bDefaultInstantiated = false;

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  private final ICommonsMap <String, ISupplier <? extends ISMPManagerProvider>> m_aMap = new CommonsLinkedHashMap <> ();

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
  @Nonnull
  public static SMPBackendRegistry getInstance ()
  {
    final SMPBackendRegistry ret = SingletonHolder.s_aInstance;
    s_bDefaultInstantiated = true;
    return ret;
  }

  public void registerSMPBackend (@Nonnull @Nonempty final String sID,
                                  @Nonnull final ISupplier <? extends ISMPManagerProvider> aFactory)
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
   * Find and instantiate the {@link ISMPManagerProvider} for the provided
   * backend ID.
   *
   * @param sBackendID
   *        The backend ID to be searched. May be <code>null</code> or empty.
   * @return <code>null</code> if no manager provider for the provided ID is
   *         present or if the factory created a <code>null</code> instance.
   */
  @Nullable
  public ISMPManagerProvider getManagerProvider (@Nullable final String sBackendID)
  {
    if (StringHelper.hasNoText (sBackendID))
      return null;

    final ISupplier <? extends ISMPManagerProvider> aFactory = m_aRWLock.readLocked ( () -> m_aMap.get (sBackendID));
    return aFactory == null ? null : aFactory.get ();
  }

  /**
   * @return A set with all registered backend IDs. Never <code>null</code> but
   *         maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllBackendIDs ()
  {
    return m_aRWLock.readLocked ((Supplier <ICommonsSet <String>>) m_aMap::copyOfKeySet);
  }

  /**
   * Remove all registered backends and re-read the information from the SPI
   * providers.
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
