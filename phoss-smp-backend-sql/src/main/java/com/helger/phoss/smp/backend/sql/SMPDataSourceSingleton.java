package com.helger.phoss.smp.backend.sql;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;

public final class SMPDataSourceSingleton extends AbstractGlobalSingleton
{
  private final SMPDataSourceProvider m_aDSP = new SMPDataSourceProvider ();

  @Deprecated
  @UsedViaReflection
  public SMPDataSourceSingleton ()
  {}

  @Nonnull
  public static SMPDataSourceSingleton getInstance ()
  {
    return getGlobalSingleton (SMPDataSourceSingleton.class);
  }

  @Override
  protected void onBeforeDestroy (@Nonnull final IScope aScopeToBeDestroyed) throws Exception
  {
    StreamHelper.close (m_aDSP);
  }

  @Nonnull
  public SMPDataSourceProvider getDataSourceProvider ()
  {
    return m_aDSP;
  }
}
