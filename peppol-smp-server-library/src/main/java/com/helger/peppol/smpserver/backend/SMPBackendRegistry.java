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
package com.helger.peppol.smpserver.backend;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.factory.IFactory;
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

  private static final Logger s_aLogger = LoggerFactory.getLogger (SMPBackendRegistry.class);
  private static boolean s_bDefaultInstantiated = false;

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  private final Map <String, IFactory <? extends ISMPManagerProvider>> m_aMap = new LinkedHashMap <> ();

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
                                  @Nonnull final IFactory <? extends ISMPManagerProvider> aFactory)
  {
    ValueEnforcer.notEmpty (sID, "ID");
    ValueEnforcer.notNull (aFactory, "Factory");

    m_aRWLock.writeLock ().lock ();
    try
    {
      if (m_aMap.containsKey (sID))
        throw new IllegalArgumentException ("Another SMP backend with ID '" + sID + "' is already registered!");
      m_aMap.put (sID, aFactory);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
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

    m_aRWLock.readLock ().lock ();
    try
    {
      final IFactory <? extends ISMPManagerProvider> aFactory = m_aMap.get (sBackendID);
      return aFactory == null ? null : aFactory.create ();
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  /**
   * @return A set with all registered backend IDs. Never <code>null</code> but
   *         maybe empty.
   */
  @Nonnull
  @ReturnsMutableCopy
  public Set <String> getAllBackendIDs ()
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return CollectionHelper.newOrderedSet (m_aMap.keySet ());
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  /**
   * Remove all registered backends and re-read the information from the SPI
   * providers.
   */
  public void reinitialize ()
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      m_aMap.clear ();

      // register all SPI implementations
      for (final ISMPBackendRegistrarSPI aSPI : ServiceLoaderHelper.getAllSPIImplementations (ISMPBackendRegistrarSPI.class))
      {
        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Calling registerSMPBackend on " + aSPI.getClass ().getName ());
        aSPI.registerSMPBackend (this);
      }

      if (s_aLogger.isDebugEnabled ())
        s_aLogger.debug (m_aMap.size () + " SMP backends registered");
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("map", m_aMap).toString ();
  }
}
