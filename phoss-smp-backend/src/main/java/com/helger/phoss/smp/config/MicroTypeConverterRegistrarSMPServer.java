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
package com.helger.phoss.smp.config;

import com.helger.annotation.style.IsSPIImplementation;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardMicroTypeConverter;
import com.helger.phoss.smp.domain.pmigration.SMPParticipantMigration;
import com.helger.phoss.smp.domain.pmigration.SMPParticipantMigrationMicroTypeConverter;
import com.helger.phoss.smp.domain.redirect.SMPRedirect;
import com.helger.phoss.smp.domain.redirect.SMPRedirectMicroTypeConverter;
import com.helger.phoss.smp.domain.servicegroup.SMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.SMPServiceGroupMicroTypeConverter;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpointMicroTypeConverter;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcessMicroTypeConverter;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformationMicroTypeConverter;
import com.helger.xml.microdom.convert.IMicroTypeConverterRegistrarSPI;
import com.helger.xml.microdom.convert.IMicroTypeConverterRegistry;

import jakarta.annotation.Nonnull;

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
    aRegistry.registerMicroElementTypeConverter (SMPParticipantMigration.class,
                                                 new SMPParticipantMigrationMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (SMPProcess.class, new SMPProcessMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (SMPRedirect.class, new SMPRedirectMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (SMPServiceGroup.class, new SMPServiceGroupMicroTypeConverter ());
    aRegistry.registerMicroElementTypeConverter (SMPServiceInformation.class,
                                                 new SMPServiceInformationMicroTypeConverter ());
  }
}
