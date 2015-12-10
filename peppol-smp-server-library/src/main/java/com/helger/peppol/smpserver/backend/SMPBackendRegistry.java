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
}
