/**
 * Copyright (C) 2015-2021 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.migration;

import javax.annotation.concurrent.Immutable;

/**
 * This class contains global SMP server migration IDs.
 *
 * @author Philip Helger
 */
@Immutable
public final class CSMPServerMigrations
{
  public static final String MIGRATION_ID_SQL_DBUSER_TO_REGULAR_USERS = "sql.db-users-to-regular-users";

  private CSMPServerMigrations ()
  {}
}
