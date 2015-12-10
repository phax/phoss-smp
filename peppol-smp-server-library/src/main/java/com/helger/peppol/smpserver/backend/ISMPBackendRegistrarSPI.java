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
   * Register your backend at the provided {@link ISMPBackendRegistry}.
   *
   * @param aRegistry
   *        The registry to register your backends at. Never <code>null</code>.
   */
  void registerSMPBackend (@Nonnull ISMPBackendRegistry aRegistry);
}
