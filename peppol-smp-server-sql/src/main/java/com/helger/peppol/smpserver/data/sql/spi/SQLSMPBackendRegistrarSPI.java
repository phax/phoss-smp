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
package com.helger.peppol.smpserver.data.sql.spi;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.peppol.smpserver.backend.ISMPBackendRegistrarSPI;
import com.helger.peppol.smpserver.backend.ISMPBackendRegistry;
import com.helger.peppol.smpserver.data.sql.mgr.SQLManagerProvider;

/**
 * Register the SQL backend to the global SMP backend registry.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class SQLSMPBackendRegistrarSPI implements ISMPBackendRegistrarSPI
{
  public void registerSMPBackend (@Nonnull final ISMPBackendRegistry aRegistry)
  {
    aRegistry.registerSMPBackend ("sql", SQLManagerProvider::new);
  }
}
