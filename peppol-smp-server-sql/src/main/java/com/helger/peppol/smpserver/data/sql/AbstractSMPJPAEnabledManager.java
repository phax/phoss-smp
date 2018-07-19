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
package com.helger.peppol.smpserver.data.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.db.jpa.JPAEnabledManager;

public abstract class AbstractSMPJPAEnabledManager extends JPAEnabledManager
{
  protected static final Logger LOGGER = LoggerFactory.getLogger (AbstractSMPJPAEnabledManager.class);

  public AbstractSMPJPAEnabledManager ()
  {
    super ( () -> SMPEntityManagerWrapper.getInstance ().getEntityManager ());

    // To avoid some EclipseLink logging issues
    setUseTransactionsForSelect (true);
  }
}
