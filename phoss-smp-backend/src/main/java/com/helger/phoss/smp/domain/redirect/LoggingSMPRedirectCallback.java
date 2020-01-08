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
package com.helger.phoss.smp.domain.redirect;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging implementation of {@link ISMPRedirectCallback}
 *
 * @author Philip Helger
 */
public class LoggingSMPRedirectCallback implements ISMPRedirectCallback
{
  private static final Logger LOGGER = LoggerFactory.getLogger (LoggingSMPRedirectCallback.class);

  public void onSMPRedirectCreated (@Nonnull final ISMPRedirect aRedirect)
  {
    LOGGER.info ("Successfully Created Redirect '" + aRedirect.getID () + "'");
  }

  public void onSMPRedirectUpdated (@Nonnull final ISMPRedirect aRedirect)
  {
    LOGGER.info ("Successfully Updated Redirect with ID '" + aRedirect.getID () + "'");
  }

  public void onSMPRedirectDeleted (@Nonnull final ISMPRedirect aRedirect)
  {
    LOGGER.info ("Successfully Deleted Redirect with ID '" + aRedirect.getID () + "'");
  }
}
