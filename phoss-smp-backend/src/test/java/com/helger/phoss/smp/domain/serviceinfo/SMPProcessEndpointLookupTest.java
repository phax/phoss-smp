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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.peppolid.simple.process.SimpleProcessIdentifier;

/**
 * Test class for {@link SMPProcess#getAllEndpointsOfTransportProfile(String)}.
 *
 * @author Greg Taube
 */
public final class SMPProcessEndpointLookupTest
{
  @Test
  public void testAlwaysReturnsAList ()
  {
    final SMPProcess aProcess = new SMPProcess (new SimpleProcessIdentifier (PeppolIdentifierHelper.DEFAULT_PROCESS_SCHEME,
                                                                             "test-process"),
                                                null,
                                                null);

    assertTrue (aProcess.getAllEndpointsOfTransportProfile (null).isEmpty ());
    assertTrue (aProcess.getAllEndpointsOfTransportProfile ("").isEmpty ());
    assertTrue (aProcess.getAllEndpointsOfTransportProfile ("unknown").isEmpty ());

    final SMPEndpoint aEndpoint = new SMPEndpoint ("endpoint-1",
                                                   "transport-profile-1",
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
    aProcess.addEndpoint (aEndpoint);

    final var aEndpoints = aProcess.getAllEndpointsOfTransportProfile ("transport-profile-1");
    assertEquals (1, aEndpoints.size ());
    assertSame (aEndpoint, aEndpoints.getFirstOrNull ());
  }
}
