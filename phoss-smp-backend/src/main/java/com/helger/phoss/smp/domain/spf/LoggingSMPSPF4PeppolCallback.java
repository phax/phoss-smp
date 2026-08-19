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
package com.helger.phoss.smp.domain.spf;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A logging implementation of {@link ISMPSPF4PeppolPolicyCallback}.
 *
 * @author Steven Noels
 */
public class LoggingSMPSPF4PeppolCallback implements ISMPSPF4PeppolPolicyCallback
{
  private static final Logger LOGGER = LoggerFactory.getLogger (LoggingSMPSPF4PeppolCallback.class);

  public void onSMPSPFPolicyCreatedOrUpdated (@NonNull final ISMPSPF4PeppolPolicy aPolicy)
  {
    LOGGER.info ("SPF4Peppol policy for '" +
                 aPolicy.getParticipantIdentifier ().getURIEncoded () +
                 "' was created/updated with " +
                 aPolicy.getTermCount () +
                 " terms");
  }

  public void onSMPSPFPolicyDeleted (@NonNull final ISMPSPF4PeppolPolicy aPolicy)
  {
    LOGGER.info ("SPF4Peppol policy for '" + aPolicy.getParticipantIdentifier ().getURIEncoded () + "' was deleted");
  }
}
