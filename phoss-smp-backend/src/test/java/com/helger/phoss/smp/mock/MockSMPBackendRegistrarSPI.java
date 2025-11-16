/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.mock;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.base.lang.clazz.FactoryNewInstance;
import com.helger.phoss.smp.backend.ISMPBackendRegistrarSPI;
import com.helger.phoss.smp.backend.ISMPBackendRegistry;

/**
 * Register the mock backend to the global SMP backend registry.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class MockSMPBackendRegistrarSPI implements ISMPBackendRegistrarSPI
{
  public void registerSMPBackend (@NonNull final ISMPBackendRegistry aRegistry)
  {
    aRegistry.registerSMPBackend ("mock", FactoryNewInstance.create (MockSMPManagerProvider.class));
  }
}
