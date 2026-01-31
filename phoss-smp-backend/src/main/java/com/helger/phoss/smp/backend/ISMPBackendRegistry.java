/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.backend;

import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.phoss.smp.domain.ISMPManagerProvider;

/**
 * Base interface for {@link SMPBackendRegistry}.
 *
 * @author Philip Helger
 */
public interface ISMPBackendRegistry
{
  /**
   * Register a new SMP backend.
   *
   * @param sID
   *        The ID to be used to identify this backend. May neither be
   *        <code>null</code> nor empty. This is the ID that must be referenced
   *        from the SMP configuration file.
   * @param aFactory
   *        The factory to be used to create the backend manager instance. May
   *        not be <code>null</code>.
   * @throws IllegalArgumentException
   *         If another backend with the same ID is already registered.
   */
  void registerSMPBackend (@NonNull @Nonempty String sID, @NonNull Supplier <? extends ISMPManagerProvider> aFactory);
}
