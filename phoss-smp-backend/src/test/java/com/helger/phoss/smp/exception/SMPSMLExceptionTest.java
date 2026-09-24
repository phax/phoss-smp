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
package com.helger.phoss.smp.exception;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * Test class for class {@link SMPSMLException}.
 *
 * @author Philip Helger
 */
public class SMPSMLExceptionTest
{
  @Test
  public void testCauseMessageIsAppended ()
  {
    final Exception aCause = new IllegalStateException ("Could not create business x in SML - [ERR-106] already exist");
    final SMPSMLException aEx = new SMPSMLException ("Failed to create 'x' in SML", aCause);

    assertEquals ("Failed to create 'x' in SML" +
                  " - Could not create business x in SML - [ERR-106] already exist",
                  aEx.getMessage ());
    assertSame (aCause, aEx.getCause ());
  }

  @Test
  public void testCauseWithoutMessage ()
  {
    final Exception aCause = new IllegalStateException ();
    final SMPSMLException aEx = new SMPSMLException ("Failed to create 'x' in SML", aCause);

    assertEquals ("Failed to create 'x' in SML", aEx.getMessage ());
    assertSame (aCause, aEx.getCause ());
  }
}
