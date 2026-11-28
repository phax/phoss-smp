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
package com.helger.phoss.smp.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.junit.Test;

import com.helger.datetime.helper.PDTFactory;

/**
 * Test class for class {@link ISMPTimestampManager}.
 *
 * @author Philip Helger
 */
public final class ISMPTimestampManagerTest
{
  @Test
  public void testDefaultInstance ()
  {
    final ISMPTimestampManager aMgr = ISMPTimestampManager.createDefaultInstance ();
    final OffsetDateTime aNow = aMgr.getCurrentDateTime ();
    assertNotNull (aNow);
    // Milliseconds only
    assertEquals (0, aNow.getNano () % 1_000_000);
    // Default time zone
    assertEquals (PDTFactory.getCurrentOffsetDateTime ().getOffset (), aNow.getOffset ());

    final LocalDateTime aLocalNow = aMgr.getCurrentLocalDateTime ();
    assertEquals (0, aLocalNow.getNano () % 1_000_000);
    assertFalse (aLocalNow.isBefore (aNow.toLocalDateTime ()));

    assertNotNull (aMgr.getCurrentXMLDateTime ());
    assertNotNull (aMgr.getCurrentDate ());
  }

  @Test
  public void testAvailableBeforeInitialization ()
  {
    // Must never trigger the initialization of SMPMetaManager
    assertNotNull (SMPMetaManager.getTimestampMgr ());
    assertNotNull (SMPMetaManager.getTimestampMgr ().getCurrentLocalDateTime ());
  }
}
