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
package com.helger.phoss.smp.domain.businesscard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;

/**
 * Logging implementation of {@link ISMPBusinessCardCallback}
 *
 * @author Philip Helger
 * @since 5.2.6
 */
public class LoggingSMPBusinessCardCallback implements ISMPBusinessCardCallback
{
  private static final Logger LOGGER = LoggerFactory.getLogger (LoggingSMPBusinessCardCallback.class);

  public void onSMPBusinessCardCreatedOrUpdated (@Nonnull final ISMPBusinessCard aBusinessCard)
  {
    LOGGER.info ("Successfully Created/Updated BusinessCard with ID '" +
                 aBusinessCard.getParticipantIdentifier ().getURIEncoded () +
                 "'");
  }

  public void onSMPBusinessCardDeleted (@Nonnull final ISMPBusinessCard aBusinessCard)
  {
    LOGGER.info ("Successfully Deleted BusinessCard with ID '" +
                 aBusinessCard.getParticipantIdentifier ().getURIEncoded () +
                 "'");
  }
}
