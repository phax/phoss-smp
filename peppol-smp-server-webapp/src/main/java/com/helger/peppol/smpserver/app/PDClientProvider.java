package com.helger.peppol.smpserver.app;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.scope.IScope;
import com.helger.pd.client.PDClient;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.web.scope.singleton.AbstractGlobalWebSingleton;

/**
 * A singleton that keeps track of the {@link PDClient} creation (PEPPOL
 * Directory client). It avoid instantiating the object too often and ensures
 * the object is correctly closed upon shutdown.
 *
 * @author Philip Helger
 */
public final class PDClientProvider extends AbstractGlobalWebSingleton
{
  private PDClient m_aPDClient;

  @Deprecated
  @UsedViaReflection
  public PDClientProvider ()
  {}

  @Nonnull
  public static PDClientProvider getInstance ()
  {
    return getGlobalSingleton (PDClientProvider.class);
  }

  @Override
  protected void onDestroy (@Nonnull final IScope aScopeInDestruction)
  {
    m_aRWLock.writeLocked ( () -> {
      StreamHelper.close (m_aPDClient);
      m_aPDClient = null;
    });
  }

  /**
   * Reset the existing client in case the SMP settings changed.
   */
  public void resetPDClient ()
  {
    m_aRWLock.writeLocked ( () -> m_aPDClient = null);
  }

  /**
   * @return The {@link PDClient} to be used with the current settings. Never
   *         <code>null</code>.
   */
  @Nonnull
  public PDClient getPDClient ()
  {
    PDClient ret = m_aRWLock.readLocked ( () -> m_aPDClient);
    if (ret == null)
      ret = m_aRWLock.writeLocked ( () -> {
        if (m_aPDClient == null)
        {
          // Create a new one
          m_aPDClient = new PDClient (SMPMetaManager.getSettings ().getPEPPOLDirectoryHostName ());
        }
        return m_aPDClient;
      });
    return ret;
  }
}
