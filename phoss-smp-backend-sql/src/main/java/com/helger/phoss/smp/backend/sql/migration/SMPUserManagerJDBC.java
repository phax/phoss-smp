/*
 * Copyright (C) 2019-2025 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.sql.migration;

import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.type.ObjectType;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.db.api.EDatabaseSystemType;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.db.jdbc.mgr.AbstractJDBCEnabledManager;
import com.helger.phoss.smp.backend.sql.SMPDataSourceSingleton;
import com.helger.phoss.smp.backend.sql.domain.DBUser;

import jakarta.annotation.Nonnull;

/**
 * This is only used in the migration.
 *
 * @author Philip Helger
 * @since 5.3.0
 */
final class SMPUserManagerJDBC extends AbstractJDBCEnabledManager
{
  public static final ObjectType OT = new ObjectType ("smpuser");

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPUserManagerJDBC.class);

  /**
   * Constructor
   *
   * @param aDBExecSupplier
   *        The supplier for {@link DBExecutor} objects. May not be <code>null</code>.
   */
  public SMPUserManagerJDBC (@Nonnull final Supplier <? extends DBExecutor> aDBExecSupplier)
  {
    super (aDBExecSupplier);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <DBUser> getAllUsers ()
  {
    final ICommonsList <DBUser> ret = new CommonsArrayList <> ();
    // Plaintext password....
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT username, password FROM smp_user");
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
        ret.add (new DBUser (aRow.getAsString (0), aRow.getAsString (1)));
    return ret;
  }

  public void updateOwnershipsAndKillUsers (@Nonnull final ICommonsMap <String, String> aOldToNewUserNameMap)
  {
    ValueEnforcer.notNull (aOldToNewUserNameMap, "OldToNewUserNameMap");

    final DBExecutor aExecutor = newExecutor ();
    aExecutor.performInTransaction ( () -> {
      // Drop the Foreign Key Constraint - do this all the time
      try
      {
        final EDatabaseSystemType eDBType = SMPDataSourceSingleton.getDatabaseType ();
        switch (eDBType)
        {
          case MYSQL:
            aExecutor.executeStatement ("ALTER TABLE smp_ownership DROP FOREIGN KEY FK_smp_ownership_username");
            break;
          case POSTGRESQL:
            aExecutor.executeStatement ("ALTER TABLE smp_ownership DROP CONSTRAINT FK_smp_ownership_username");
            break;
          case ORACLE:
          case DB2:
            // No such constraint
            break;
          default:
            throw new IllegalStateException ("The migration code for DB type " + eDBType + " is missing");
        }
      }
      catch (final RuntimeException ex)
      {
        LOGGER.warn ("Error in droping constraints", ex);
      }

      // Update user names
      for (final Map.Entry <String, String> aEntry : aOldToNewUserNameMap.entrySet ())
      {
        final String sOld = aEntry.getKey ();
        final String sNew = aEntry.getValue ();
        aExecutor.insertOrUpdateOrDelete ("UPDATE smp_ownership SET username=? WHERE username=?",
                                          new ConstantPreparedStatementDataProvider (sNew, sOld));
      }

      // Delete the table
      try
      {
        aExecutor.executeStatement ("DROP TABLE smp_user");
      }
      catch (final RuntimeException ex)
      {
        LOGGER.warn ("Error in droping table smp_user", ex);
      }
    });
  }
}
