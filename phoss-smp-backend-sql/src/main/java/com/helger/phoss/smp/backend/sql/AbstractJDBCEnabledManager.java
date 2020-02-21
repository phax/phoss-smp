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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.db.jdbc.executor.DBExecutor;

public abstract class AbstractJDBCEnabledManager
{
  protected static final Logger LOGGER = LoggerFactory.getLogger (AbstractJDBCEnabledManager.class);
  private final DBExecutor m_aDBExec;

  public AbstractJDBCEnabledManager ()
  {
    m_aDBExec = new DBExecutor (SMPDataSourceSingleton.getInstance ().getDataSourceProvider ());
  }

  @Nonnull
  protected final DBExecutor executor ()
  {
    return m_aDBExec;
  }
}
