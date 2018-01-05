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
package com.helger.peppol.smpserver.config;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardMicroTypeConverter;
import com.helger.peppol.smpserver.domain.redirect.SMPRedirect;
import com.helger.peppol.smpserver.domain.redirect.SMPRedirectMicroTypeConverter;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroupMicroTypeConverter;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPEndpointMicroTypeConverter;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPProcessMicroTypeConverter;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformationMicroTypeConverter;
import com.helger.xml.microdom.convert.IMicroTypeConverterRegistrarSPI;
import com.helger.xml.microdom.convert.IMicroTypeConverterRegistry;

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
    aRegistry.registerMicroElementTypeConverter (SMPBusinessCard.class, new SMPBusinessCardMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (SMPEndpoint.class, new SMPEndpointMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (SMPProcess.class, new SMPProcessMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (SMPRedirect.class, new SMPRedirectMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (SMPServiceGroup.class, new SMPServiceGroupMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (SMPServiceInformation.class,
                                                 new SMPServiceInformationMicroTypeConverter ());
  }
}
