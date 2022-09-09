package com.helger.phoss.smp.ui.secure;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.phoss.smp.config.SMPHttpConfiguration;
import com.helger.scope.singleton.AbstractSessionSingleton;

/**
 * A helper class that checks once per session if proxy information are
 * configured or not. Usually this information does not change, it it is not
 * worth the effort to query that in every request.
 *
 * @author Philip Helger
 */
public final class SecureSessionState extends AbstractSessionSingleton
{
  private final boolean m_bHttpProxyEnabled;
  private final boolean m_bHttpsProxyEnabled;

  @Deprecated
  @UsedViaReflection
  public SecureSessionState ()
  {
    m_bHttpProxyEnabled = SMPHttpConfiguration.getAsHttpProxySettings () != null;
    m_bHttpsProxyEnabled = SMPHttpConfiguration.getAsHttpsProxySettings () != null;
  }

  @Nonnull
  public static SecureSessionState getInstance ()
  {
    return getSessionSingleton (SecureSessionState.class);
  }

  public boolean isHttpProxyEnabled ()
  {
    return m_bHttpProxyEnabled;
  }

  public boolean isHttpsProxyEnabled ()
  {
    return m_bHttpsProxyEnabled;
  }

  public boolean isAnyHttpProxyEnabled ()
  {
    return m_bHttpProxyEnabled || m_bHttpsProxyEnabled;
  }
}
