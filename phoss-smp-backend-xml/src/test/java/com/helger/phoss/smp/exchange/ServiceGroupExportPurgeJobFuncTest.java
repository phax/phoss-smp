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
package com.helger.phoss.smp.exchange;

import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.quartz.TriggerKey;
import com.helger.schedule.quartz.GlobalQuartzScheduler;

/**
 * Test class for class {@link ServiceGroupExportPurgeJob}.
 *
 * @author Philip Helger
 */
public final class ServiceGroupExportPurgeJobFuncTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testScheduleAndUnschedule ()
  {
    // This mainly ensures, that the Quartz scheduler is usable at all and that the trigger can be
    // built as configured
    final TriggerKey aTriggerKey = ServiceGroupExportPurgeJob.schedule ();
    try
    {
      assertNotNull (aTriggerKey);
    }
    finally
    {
      GlobalQuartzScheduler.getInstance ().unscheduleJob (aTriggerKey);
    }
  }
}
