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

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIInterface;

/**
 * SPI interface to be implemented by SMP backend modules. They are than
 * automatically registered by the {@link SMPBackendRegistry}.
 *
 * @author Philip Helger
 */
@IsSPIInterface
public interface ISMPBackendRegistrarSPI
{
  /**
   * Register your backend(s) at the provided {@link ISMPBackendRegistry}.
   *
   * @param aRegistry
   *        The registry to register your backend(s) at. Never <code>null</code>
   *        .
   */
  void registerSMPBackend (@Nonnull ISMPBackendRegistry aRegistry);
}
