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
package com.helger.peppol.smpserver.smlhook;

import org.junit.Ignore;
import org.junit.Test;

import com.helger.peppol.identifier.factory.PeppolIdentifierFactory;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

/**
 * Test class for class {@link RegistrationHookWriteToSML}.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class RegistrationHookWriteToSMLTest
{
  @Test
  @Ignore ("Potentially modifies the DNS!")
  public void testCreateAndDelete () throws RegistrationHookException
  {
    final RegistrationHookWriteToSML aHook = new RegistrationHookWriteToSML ();
    final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("0088:12345test");
    aHook.createServiceGroup (aPI);
    aHook.deleteServiceGroup (aPI);
    // Throws ExceptionInInitializerError:
    // Happens when no keystore is present!
  }
}
