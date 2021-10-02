/**
 * Copyright (C) 2019-2021 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.backend.sql.audit;

import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.type.ObjectType;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.json.JsonArray;
import com.helger.phoss.smp.backend.sql.mgr.AbstractJDBCEnabledManager;
import com.helger.photon.audit.EAuditActionType;
import com.helger.photon.audit.IAuditor;

/**
 * A special implementation of {@link IAuditor} writing data to a SQL table
 *
 * @author Philip Helger
 */
public class JDBCAuditor extends AbstractJDBCEnabledManager implements IAuditor
{
  public static final int OBJECT_TYPE_MAX_LENGTH = 100;
  private static final Logger LOGGER = LoggerFactory.getLogger (JDBCAuditor.class);

  /**
   * Constructor
   *
   * @param aDBExecSupplier
   *        The supplier for {@link DBExecutor} objects. May not be
   *        <code>null</code>.
   */
  public JDBCAuditor (final Supplier <? extends DBExecutor> aDBExecSupplier)
  {
    super (aDBExecSupplier);
  }

  public void createAuditItem (@Nonnull final EAuditActionType eActionType,
                               @Nonnull final ESuccess eSuccess,
                               @Nullable final ObjectType aActionObjectType,
                               @Nullable final String sAction,
                               @Nullable final Object... aArgs)
  {
    // Combine arguments
    final String sArgs = new JsonArray ().addAll (aArgs).getAsJsonString ();

    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eDBSuccess = aExecutor.performInTransaction ( () -> {
      // Create new
      final long nCreated = aExecutor.insertOrUpdateOrDelete ("INSERT INTO smp_audit (dt, actiontype, success, objtype, action, args) VALUES (?, ?, ?, ?, ?, ?)",
                                                              new ConstantPreparedStatementDataProvider (toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                         getTrimmedToLength (eActionType.getID (),
                                                                                                                             10),
                                                                                                         Boolean.valueOf (eSuccess.isSuccess ()),
                                                                                                         getTrimmedToLength (aActionObjectType != null ? aActionObjectType.getName ()
                                                                                                                                                       : null,
                                                                                                                             100),
                                                                                                         getTrimmedToLength (StringHelper.hasText (sAction) ? sAction
                                                                                                                                                            : null,
                                                                                                                             100),
                                                                                                         sArgs));
      if (nCreated != 1)
        throw new IllegalStateException ("Failed to create new DB entry (" + nCreated + ")");
    });

    if (eDBSuccess.isFailure ())
      LOGGER.error ("Failed to write audit item to DB");
  }
}
