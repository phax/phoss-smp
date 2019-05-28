/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.serviceinfo;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging implementation of {@link ISMPServiceInformationCallback}
 *
 * @author Philip Helger
 */
public class LoggingSMPServiceInformationCallback implements ISMPServiceInformationCallback
{
  private static final Logger LOGGER = LoggerFactory.getLogger (LoggingSMPServiceInformationCallback.class);

  public void onSMPServiceInformationCreated (@Nonnull final ISMPServiceInformation aServiceInformation)
  {
    LOGGER.info ("Successfully Created ServiceInformation at '" +
                 aServiceInformation.getServiceGroupID () +
                 "' for " +
                 aServiceInformation.getDocumentTypeIdentifier ().getURIEncoded ());
  }

  public void onSMPServiceInformationUpdated (@Nonnull final ISMPServiceInformation aServiceInformation)
  {
    LOGGER.info ("Successfully Updated ServiceInformation at '" +
                 aServiceInformation.getServiceGroupID () +
                 "' for " +
                 aServiceInformation.getDocumentTypeIdentifier ().getURIEncoded ());
  }

  public void onSMPServiceInformationDeleted (@Nonnull final ISMPServiceInformation aServiceInformation)
  {
    LOGGER.info ("Successfully Deleted ServiceInformation at '" +
                 aServiceInformation.getServiceGroupID () +
                 "' for " +
                 aServiceInformation.getDocumentTypeIdentifier ().getURIEncoded ());
  }
}
