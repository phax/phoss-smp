/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.data.xml.spi;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.factory.FactoryNewInstance;
import com.helger.peppol.smpserver.backend.ISMPBackendRegistrarSPI;
import com.helger.peppol.smpserver.backend.ISMPBackendRegistry;
import com.helger.peppol.smpserver.data.xml.mgr.XMLManagerProvider;

/**
 * Register the XML backend to the global SMP backend registry.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class XMLSMPBackendRegistrarSPI implements ISMPBackendRegistrarSPI
{
  public void registerSMPBackend (@Nonnull final ISMPBackendRegistry aRegistry)
  {
    aRegistry.registerSMPBackend ("xml", FactoryNewInstance.create (XMLManagerProvider.class));
  }
}
