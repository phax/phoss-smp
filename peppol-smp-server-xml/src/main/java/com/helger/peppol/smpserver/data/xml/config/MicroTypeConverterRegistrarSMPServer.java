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
package com.helger.peppol.smpserver.data.xml.config;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.microdom.convert.IMicroTypeConverterRegistrarSPI;
import com.helger.commons.microdom.convert.IMicroTypeConverterRegistry;
import com.helger.peppol.smpserver.domain.redirect.SMPRedirect;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroup;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformation;

/**
 * Special micro type converter for this project.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class MicroTypeConverterRegistrarSMPServer implements IMicroTypeConverterRegistrarSPI
{
  public void registerMicroTypeConverter (@Nonnull final IMicroTypeConverterRegistry aRegistry)
  {
    aRegistry.registerMicroElementTypeConverter (SMPEndpoint.class, new SMPEndpointMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (SMPProcess.class, new SMPProcessMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (SMPRedirect.class, new SMPRedirectMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (SMPServiceGroup.class, new SMPServiceGroupMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (SMPServiceInformation.class,
                                                 new SMPServiceInformationMicroTypeConverter ());
  }
}
