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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.datetime.helper.PDTFactory;
import com.helger.quartz.DisallowConcurrentExecution;
import com.helger.quartz.IJobExecutionContext;
import com.helger.quartz.ITrigger.EMisfireInstruction;
import com.helger.quartz.JobDataMap;
import com.helger.quartz.SimpleScheduleBuilder;
import com.helger.quartz.TriggerKey;
import com.helger.schedule.quartz.GlobalQuartzScheduler;
import com.helger.schedule.quartz.trigger.JDK8TriggerBuilder;
import com.helger.web.scope.util.AbstractScopeAwareJob;

/**
 * A scheduled job that deletes the outdated Service Group export files once a day.
 *
 * @author Philip Helger
 * @since 8.3.0
 * @see ServiceGroupExportJob#purgeOldExportFiles()
 */
@DisallowConcurrentExecution
public final class ServiceGroupExportPurgeJob extends AbstractScopeAwareJob
{
  /** The hour of the day at which the purge is performed */
  public static final int SCHEDULE_HOUR_OF_DAY = 2;

  private static final Logger LOGGER = LoggerFactory.getLogger (ServiceGroupExportPurgeJob.class);

  @Override
  protected void onExecute (@NonNull final JobDataMap aJobDataMap, @NonNull final IJobExecutionContext aContext)
  {
    final int nDeleted = ServiceGroupExportJob.purgeOldExportFiles ();
    if (nDeleted > 0)
      LOGGER.info ("Deleted " +
                   nDeleted +
                   " outdated Service Group export " +
                   (nDeleted == 1 ? "file" : "files"));
  }

  /**
   * Schedule this job to run once a day.
   *
   * @return The created trigger key, so that the job can be unscheduled on shutdown. Never
   *         <code>null</code>.
   */
  @NonNull
  public static TriggerKey schedule ()
  {
    return GlobalQuartzScheduler.getInstance ()
                                .scheduleJob (ServiceGroupExportPurgeJob.class.getName (),
                                              JDK8TriggerBuilder.newTrigger ()
                                                                .startAt (PDTFactory.getCurrentLocalDateTime ()
                                                                                    .withHour (SCHEDULE_HOUR_OF_DAY)
                                                                                    .withMinute (0)
                                                                                    .withSecond (0)
                                                                                    .withNano (0))
                                                                .withSchedule (SimpleScheduleBuilder.repeatHourlyForever (24))
                                                                .withMisfireInstruction (EMisfireInstruction.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT),
                                              ServiceGroupExportPurgeJob.class,
                                              null);
  }
}
