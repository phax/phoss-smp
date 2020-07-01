/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
