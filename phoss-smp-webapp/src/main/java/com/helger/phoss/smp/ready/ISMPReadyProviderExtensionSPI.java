/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.ready;

import com.helger.annotation.style.IsSPIInterface;

/**
 * SPI interface for backend-specific readiness checks.
 *
 * @author vinit-thummar
 * @since 8.3.1
 */
@FunctionalInterface
@IsSPIInterface
public interface ISMPReadyProviderExtensionSPI
{
  /**
   * @return <code>true</code> if the backend is ready to serve requests, <code>false</code>
   *         otherwise.
   */
  boolean isReady ();
}
