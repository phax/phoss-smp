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
package com.helger.phoss.smp.smlhook;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.helger.peppol.sml.SMLInfo;
import com.helger.peppol.smlclient.ManageParticipantIdentifierServiceCaller;

/**
 * Test class for class {@link SmpSmlHelper}.
 *
 * @author vinit-thummar
 */
public final class SmpSmlHelperTest
{
  private static ManageParticipantIdentifierServiceCaller _createCaller (final String sManagementServiceURL)
  {
    return SmpSmlHelper.createSMLCallerPI (SMLInfo.builder ()
                                                 .id ("test")
                                                 .displayName ("Test")
                                                 .dnsZone ("example.org")
                                                 .managementServiceURL (sManagementServiceURL)
                                                 .clientCertificateRequired (false)
                                                 .build ());
  }

  @Test
  public void testRelaxedHostnameVerifierIsRestrictedToLocalhost ()
  {
    assertNotNull (_createCaller ("http://localhost").getHostnameVerifier ());
    assertNotNull (_createCaller ("http://LOCALHOST").getHostnameVerifier ());
    assertNotNull (_createCaller ("http://127.0.0.1").getHostnameVerifier ());

    assertNull (_createCaller ("http://localhost.example.org").getHostnameVerifier ());
    assertNull (_createCaller ("http://127.0.0.1.example.org").getHostnameVerifier ());
    assertNull (_createCaller ("http://localhost@example.org").getHostnameVerifier ());
    assertNull (_createCaller ("http://example.org//localhost").getHostnameVerifier ());
  }
}
