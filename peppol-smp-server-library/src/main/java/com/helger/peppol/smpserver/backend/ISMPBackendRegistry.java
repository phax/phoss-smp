/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.peppol.smpserver.backend;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.factory.IFactory;
import com.helger.peppol.smpserver.domain.ISMPManagerProvider;

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
   *        <code>null</code> nor empty.
   * @param aFactory
   *        The factory to be used to create the backend manager instance. May
   *        not be <code>null</code>.
   * @throws IllegalArgumentException
   *         If another backend with the same ID is already registered.
   */
  void registerSMPBackend (@Nonnull @Nonempty String sID, @Nonnull IFactory <? extends ISMPManagerProvider> aFactory);
}
