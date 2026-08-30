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
 * An SPI interface to be implemented by the real backends to determine, whether the SMP is
 * currently able to serve requests. Every backend must provide exactly one implementation - a
 * backend without an external dependency simply returns <code>true</code>. If no implementation is
 * found at all, the SMP is considered to be not ready.<br>
 * Implementations are invoked on every readiness request, so they must be cheap and they must not
 * block indefinitely.
 *
 * @author vinit-thummar
 * @since 8.3.1
 */
@IsSPIInterface
public interface ISMPReadyProviderExtensionSPI
{
  /**
   * @return <code>true</code> if the backend is ready to serve requests, <code>false</code>
   *         otherwise.
   */
  boolean isReady ();
}
