/*
 * Copyright (C) 2015-2022 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.servicegroup;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.CSMPServer;

/**
 * Logging implementation of {@link ISMPServiceGroupCallback}
 *
 * @author Philip Helger
 */
public class LoggingSMPServiceGroupCallback implements ISMPServiceGroupCallback
{
  private static final Logger LOGGER = LoggerFactory.getLogger (LoggingSMPServiceGroupCallback.class);

  public void onSMPServiceGroupCreated (@Nonnull final ISMPServiceGroup aServiceGroup, final boolean bCreateInSML)
  {
    LOGGER.info ("Successfully Created ServiceGroup with ID '" +
                 aServiceGroup.getParticipantIdentifier ().getURIEncoded () +
                 "'" +
                 (bCreateInSML ? "" : CSMPServer.LOG_SUFFIX_NO_SML_INTERACTION));
  }

  public void onSMPServiceGroupUpdated (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    LOGGER.info ("Successfully Updated ServiceGroup with ID '" + aParticipantID.getURIEncoded () + "'");
  }

  public void onSMPServiceGroupDeleted (@Nonnull final IParticipantIdentifier aParticipantID, final boolean bDeleteInSML)
  {
    LOGGER.info ("Successfully Deleted ServiceGroup with ID '" +
                 aParticipantID.getURIEncoded () +
                 "'" +
                 (bDeleteInSML ? "" : CSMPServer.LOG_SUFFIX_NO_SML_INTERACTION));
  }
}
