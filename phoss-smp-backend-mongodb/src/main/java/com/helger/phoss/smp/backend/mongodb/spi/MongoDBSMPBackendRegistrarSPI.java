/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.mongodb.spi;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.phoss.smp.backend.ISMPBackendRegistrarSPI;
import com.helger.phoss.smp.backend.ISMPBackendRegistry;
import com.helger.phoss.smp.backend.mongodb.mgr.SMPManagerProviderMongoDB;

/**
 * Register the MongoDB backend to the global SMP backend registry.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class MongoDBSMPBackendRegistrarSPI implements ISMPBackendRegistrarSPI
{
  public static final String BACKEND_ID = "mongodb";

  public void registerSMPBackend (@NonNull final ISMPBackendRegistry aRegistry)
  {
    aRegistry.registerSMPBackend (BACKEND_ID, SMPManagerProviderMongoDB::new);
  }
}
