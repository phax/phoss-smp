/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.servicegroup;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging implementation of {@link ISMPServiceGroupCallback}
 *
 * @author Philip Helger
 */
public class LoggingSMPServiceGroupCallback implements ISMPServiceGroupCallback
{
  private static final Logger LOGGER = LoggerFactory.getLogger (LoggingSMPServiceGroupCallback.class);

  public void onSMPServiceGroupCreated (@Nonnull final ISMPServiceGroup aServiceGroup)
  {
    LOGGER.info ("Successfully Created ServiceGroup '" +
                 aServiceGroup.getParticpantIdentifier ().getURIEncoded () +
                 "'");
  }

  public void onSMPServiceGroupUpdated (@Nonnull final String sServiceGroupID)
  {
    LOGGER.info ("Successfully Updated ServiceGroup with ID '" + sServiceGroupID + "'");
  }

  public void onSMPServiceGroupDeleted (@Nonnull final String sServiceGroupID)
  {
    LOGGER.info ("Successfully Deleted ServiceGroup with ID '" + sServiceGroupID + "'");
  }
}
