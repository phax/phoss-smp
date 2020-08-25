/**
 * Copyright (C) 2019-2020 Philip Helger and contributors
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
import java.util.Optional;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.type.ObjectType;
import com.helger.commons.wrapper.Wrapper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.phoss.smp.backend.sql.EDatabaseType;
import com.helger.phoss.smp.backend.sql.domain.DBUser;
import com.helger.phoss.smp.backend.sql.mgr.AbstractJDBCEnabledManager;
import com.helger.photon.audit.AuditHelper;

/**
 * This is only used in the migration.
 *
 * @author Philip Helger
 * @since 5.3.0
 */
final class SMPUserManagerJDBC extends AbstractJDBCEnabledManager
{
  public static final ObjectType OT = new ObjectType ("smpuser");

  public SMPUserManagerJDBC (@Nonnull final EDatabaseType eDBType)
  {
    super (eDBType);
  }

  @Nonnull
  public ESuccess createUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    final Wrapper <ESuccess> ret = new Wrapper <> (ESuccess.FAILURE);
    executor ().performInTransaction ( () -> {
      if (getUserOfID (sUserName) != null)
        ret.set (ESuccess.FAILURE);
      else
      {
        final long nCount = executor ().insertOrUpdateOrDelete ("INSERT INTO smp_user (username, password) VALUES (?,?)",
                                                                new ConstantPreparedStatementDataProvider (sUserName, sPassword));
        ret.set (ESuccess.valueOf (nCount == 1));
      }
    });

    if (ret.get ().isFailure ())
    {
      AuditHelper.onAuditCreateFailure (OT, sUserName);
      return ESuccess.FAILURE;
    }

    AuditHelper.onAuditCreateSuccess (OT, sUserName);
    return ESuccess.SUCCESS;
  }

  @Nonnull
  public ESuccess updateUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    final long nCount = executor ().insertOrUpdateOrDelete ("UPDATE smp_user SET password=? WHERE username=?",
                                                            new ConstantPreparedStatementDataProvider (sPassword, sUserName));
    return ESuccess.valueOf (nCount == 1);
  }

  @Nonnull
  public EChange deleteUser (@Nullable final String sUserName)
  {
    if (StringHelper.hasNoText (sUserName))
      return EChange.UNCHANGED;

    final long nCount = executor ().insertOrUpdateOrDelete ("DELETE FROM smp_user WHERE username=?",
                                                            new ConstantPreparedStatementDataProvider (sUserName));
    return EChange.valueOf (nCount == 1);
  }

  @Nonnegative
  public long getUserCount ()
  {
    return executor ().queryCount ("SELECT COUNT(*) FROM smp_user");
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <DBUser> getAllUsers ()
  {
    final Optional <ICommonsList <DBResultRow>> aDBResult = executor ().queryAll ("SELECT username, password FROM smp_user");
    final ICommonsList <DBUser> ret = new CommonsArrayList <> ();
    if (aDBResult.isPresent ())
      for (final DBResultRow aRow : aDBResult.get ())
        ret.add (new DBUser (aRow.getAsString (0), aRow.getAsString (1)));
    return ret;
  }

  @Nullable
  public DBUser getUserOfID (@Nullable final String sUserName)
  {
    if (StringHelper.hasNoText (sUserName))
      return null;

    final Optional <DBResultRow> aDBResult = executor ().querySingle ("SELECT password FROM smp_user WHERE username=?",
                                                                      new ConstantPreparedStatementDataProvider (sUserName));
    if (!aDBResult.isPresent ())
      return null;
    return new DBUser (sUserName, aDBResult.get ().getAsString (0));
  }

  public void onMigrationUpdateOwnershipsAndKillUsers (@Nonnull final ICommonsMap <String, String> aOldToNewMap)
  {
    ValueEnforcer.notNull (aOldToNewMap, "OldToNewMap");

    executor ().performInTransaction ( () -> {
      // Drop the Foreign Key Constraint - do this all the time
      try
      {
        switch (m_eDBType)
        {
          case MYSQL:
            executor ().executeStatement ("ALTER TABLE smp_ownership DROP FOREIGN KEY FK_smp_ownership_username;");
            break;
          case POSTGRESQL:
            executor ().executeStatement ("ALTER TABLE smp_ownership DROP CONSTRAINT FK_smp_ownership_username;");
            break;
          default:
            throw new IllegalStateException ("The migration code for DB type " + m_eDBType + " is missing");
        }
      }
      catch (final RuntimeException ex)
      {
        // Ignore
      }

      // Update user names
      for (final Map.Entry <String, String> aEntry : aOldToNewMap.entrySet ())
      {
        final String sOld = aEntry.getKey ();
        final String sNew = aEntry.getValue ();
        executor ().insertOrUpdateOrDelete ("UPDATE smp_ownership SET username=? WHERE username=?",
                                            new ConstantPreparedStatementDataProvider (sNew, sOld));
      }

      try
      {
        executor ().executeStatement ("DROP TABLE smp_user;");
      }
      catch (final RuntimeException ex)
      {
        // Ignore
      }
    });
  }
}
