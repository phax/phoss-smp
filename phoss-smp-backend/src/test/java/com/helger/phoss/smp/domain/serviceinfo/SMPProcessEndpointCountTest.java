/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.serviceinfo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.peppolid.simple.process.SimpleProcessIdentifier;

/**
 * Test class for {@link SMPProcess#getEndpointCount()}.
 *
 * @author Greg Taube
 */
public final class SMPProcessEndpointCountTest
{
  private static SMPEndpoint _createEndpoint (final String sID, final String sTransportProfile)
  {
    return new SMPEndpoint (sID,
                            sTransportProfile,
                            "http://localhost/as2",
                            false,
                            null,
                            null,
                            null,
                            "cert",
                            "description",
                            "https://example.org/contact",
                            null,
                            null);
  }

  @Test
  public void testMultipleEndpointsPerTransportProfile ()
  {
    final SMPProcess aProcess = new SMPProcess (new SimpleProcessIdentifier (PeppolIdentifierHelper.DEFAULT_PROCESS_SCHEME,
                                                                             "test-process"),
                                                null,
                                                null);
    aProcess.addEndpoint (_createEndpoint ("endpoint-1", "transport-profile-1"));
    aProcess.addEndpoint (_createEndpoint ("endpoint-2", "transport-profile-1"));
    aProcess.addEndpoint (_createEndpoint ("endpoint-3", "transport-profile-2"));

    assertEquals (3, aProcess.getEndpointCount ());
  }
}
