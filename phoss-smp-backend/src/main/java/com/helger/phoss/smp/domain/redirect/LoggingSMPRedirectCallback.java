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
package com.helger.phoss.smp.domain.redirect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;

/**
 * Logging implementation of {@link ISMPRedirectCallback}
 *
 * @author Philip Helger
 */
public class LoggingSMPRedirectCallback implements ISMPRedirectCallback
{
  private static final Logger LOGGER = LoggerFactory.getLogger (LoggingSMPRedirectCallback.class);

  @Override
  public void onSMPRedirectCreated (@Nonnull final ISMPRedirect aRedirect)
  {
    LOGGER.info ("Successfully Created Redirect '" + aRedirect.getID () + "'");
  }

  @Override
  public void onSMPRedirectUpdated (@Nonnull final ISMPRedirect aRedirect)
  {
    LOGGER.info ("Successfully Updated Redirect with ID '" + aRedirect.getID () + "'");
  }

  @Override
  public void onSMPRedirectDeleted (@Nonnull final ISMPRedirect aRedirect)
  {
    LOGGER.info ("Successfully Deleted Redirect with ID '" + aRedirect.getID () + "'");
  }
}
