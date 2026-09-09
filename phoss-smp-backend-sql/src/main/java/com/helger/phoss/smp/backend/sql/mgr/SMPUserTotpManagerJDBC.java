/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.sql.mgr;

import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.numeric.mutable.MutableLong;
import com.helger.base.state.EChange;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.wrapper.Wrapper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.db.api.helper.DBValueHelper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.db.jdbc.mgr.AbstractJDBCEnabledManager;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.domain.totp.ISMPUserTotp;
import com.helger.phoss.smp.domain.totp.ISMPUserTotpManager;
import com.helger.phoss.smp.domain.totp.SMPUserTotp;
import com.helger.photon.audit.AuditHelper;

/**
 * A JDBC based implementation of the {@link ISMPUserTotpManager} interface.
 *
 * @author Philip Helger
 * @since 8.4.3
 */
public class SMPUserTotpManagerJDBC extends AbstractJDBCEnabledManager implements ISMPUserTotpManager
{
  private final String m_sTableName;

  /**
   * Constructor
   *
   * @param aDBExecSupplier
   *        The supplier for {@link DBExecutor} objects. May not be <code>null</code>.
   * @param sTableNamePrefix
   *        The table name prefix to be used. May not be <code>null</code>.
   */
  public SMPUserTotpManagerJDBC (@NonNull final Supplier <? extends DBExecutor> aDBExecSupplier,
                                 @NonNull final String sTableNamePrefix)
  {
    super (aDBExecSupplier);
    ValueEnforcer.notNull (sTableNamePrefix, "TableNamePrefix");
    m_sTableName = sTableNamePrefix + "smp_sectotp";
  }

  @NonNull
  public ISMPUserTotp createOrReplaceTotp (@NonNull @Nonempty final String sUserID,
                                           @NonNull @Nonempty final String sSecret)
  {
    ValueEnforcer.notEmpty (sUserID, "UserID");
    ValueEnforcer.notEmpty (sSecret, "Secret");

    final LocalDateTime aRegistrationDT = PDTFactory.getCurrentLocalDateTime ();

    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // An existing enrollment - confirmed or not - is always replaced
      aExecutor.insertOrUpdateOrDelete ("DELETE FROM " + m_sTableName + " WHERE userid=?",
                                        new ConstantPreparedStatementDataProvider (sUserID));

      final long nCreated = aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                                              m_sTableName +
                                                              " (userid, secret, enabled, regdt, lastslot)" +
                                                              " VALUES (?, ?, ?, ?, ?)",
                                                              new ConstantPreparedStatementDataProvider (DBValueHelper.getTrimmedToLength (sUserID,
                                                                                                                                           CSMPServer.MAX_LEN_ID),
                                                                                                         DBValueHelper.getTrimmedToLength (sSecret,
                                                                                                                                           ISMPUserTotp.SECRET_MAX_LENGTH),
                                                                                                         Boolean.FALSE,
                                                                                                         DBValueHelper.toTimestamp (aRegistrationDT),
                                                                                                         null));
      if (nCreated != 1)
        throw new IllegalStateException ("Failed to create new DB entry (" + nCreated + ")");
    });

    if (eSuccess.isFailure ())
      throw new IllegalStateException ("Failed to insert the TOTP enrollment of user '" + sUserID + "' into the DB");

    // Never audit the secret itself
    AuditHelper.onAuditCreateSuccess (SMPUserTotp.OT, sUserID);
    return new SMPUserTotp (sUserID, sSecret, false, aRegistrationDT, null);
  }

  @NonNull
  public EChange setTotpEnabled (@Nullable final String sUserID, final boolean bEnabled)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE " +
                                                              m_sTableName +
                                                              " SET enabled=? WHERE userid=?",
                                                              new ConstantPreparedStatementDataProvider (Boolean.valueOf (bEnabled),
                                                                                                         sUserID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      AuditHelper.onAuditModifyFailure (SMPUserTotp.OT, "set-enabled", sUserID, "database-error");
      return EChange.UNCHANGED;
    }
    if (aUpdated.is0 ())
      return EChange.UNCHANGED;

    AuditHelper.onAuditModifySuccess (SMPUserTotp.OT, "set-enabled", sUserID, Boolean.valueOf (bEnabled));
    return EChange.CHANGED;
  }

  @NonNull
  public EChange setTotpLastUsedTimeSlot (@Nullable final String sUserID, final long nTimeSlot)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    // Deliberately not audited - this happens on every single login
    final long nUpdated = newExecutor ().insertOrUpdateOrDelete ("UPDATE " +
                                                                 m_sTableName +
                                                                 " SET lastslot=? WHERE userid=?",
                                                                 new ConstantPreparedStatementDataProvider (Long.valueOf (nTimeSlot),
                                                                                                            sUserID));
    return EChange.valueOf (nUpdated > 0);
  }

  @NonNull
  public EChange deleteTotp (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    final long nDeleted = newExecutor ().insertOrUpdateOrDelete ("DELETE FROM " + m_sTableName + " WHERE userid=?",
                                                                 new ConstantPreparedStatementDataProvider (sUserID));
    if (nDeleted == 0)
    {
      AuditHelper.onAuditDeleteFailure (SMPUserTotp.OT, sUserID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditDeleteSuccess (SMPUserTotp.OT, sUserID);
    return EChange.CHANGED;
  }

  @Nullable
  public ISMPUserTotp getTotpOfUserID (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return null;

    final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
    newExecutor ().querySingle ("SELECT secret, enabled, regdt, lastslot FROM " + m_sTableName + " WHERE userid=?",
                                new ConstantPreparedStatementDataProvider (sUserID),
                                aDBResult::set);
    if (aDBResult.isNotSet ())
      return null;

    final DBResultRow aRow = aDBResult.get ();
    return new SMPUserTotp (sUserID,
                            aRow.getAsString (0),
                            aRow.getAsBoolean (1, false),
                            aRow.getAsLocalDateTime (2),
                            aRow.getAsLongObj (3));
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPUserTotp> getAllTotps ()
  {
    final ICommonsList <ISMPUserTotp> ret = new CommonsArrayList <> ();
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT userid, secret, enabled, regdt, lastslot FROM " +
                                                                          m_sTableName);
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
        ret.add (new SMPUserTotp (aRow.getAsString (0),
                                  aRow.getAsString (1),
                                  aRow.getAsBoolean (2, false),
                                  aRow.getAsLocalDateTime (3),
                                  aRow.getAsLongObj (4)));
    return ret;
  }
}
