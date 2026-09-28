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
package com.helger.phoss.smp.smlsync;

import com.helger.annotation.concurrent.Immutable;
import com.helger.photon.security.lock.SingleRunLock;

/**
 * Constants for the reconciliation of the local Service Groups with the SML.
 *
 * @author Philip Helger
 * @since 8.5.1
 */
@Immutable
public final class CSMLSync
{
  /**
   * The process wide lock that ensures that only a single participant wide SML operation runs at a
   * time. This is deliberately shared by all of them - a bulk repair that runs while a
   * reconciliation is in the middle of reading the SML list would act on an inconsistent picture.
   * Note that the Service Group export uses its own lock, because it performs no SML call at all.
   */
  public static final SingleRunLock LOCK = new SingleRunLock ("SML bulk operation");

  /** The name of the directory below the data path, in which the reports are created */
  public static final String SYNC_DIRECTORY = "sml-sync";

  /** The prefix of all created report files */
  public static final String SYNC_FILENAME_PREFIX = "phoss-smp-sml-sync-";

  /** The extension of all created report files */
  public static final String SYNC_FILENAME_EXTENSION = ".zip";

  /** The ZIP entry holding the run metadata and the counts */
  public static final String ENTRY_SUMMARY = "summary.xml";

  /**
   * The ZIP entry holding all participants the SML has registered for this SMP, one per line. This
   * is the input for restoring the participants after an SMP was unregistered from the SML, because
   * unregistering deletes all of them.
   */
  public static final String ENTRY_ALL_IN_SML = "all-in-sml.txt";

  /** The ZIP entry holding the participants that are missing at the SML, one per line */
  public static final String ENTRY_MISSING_IN_SML = "missing-in-sml.txt";

  /** The ZIP entry holding the participants the SML holds but this SMP does not, one per line */
  public static final String ENTRY_ORPHANS_IN_SML = "orphans-in-sml.txt";

  private CSMLSync ()
  {}
}
