/*
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.type.ObjectType;
import com.helger.commons.wrapper.Wrapper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.phoss.smp.backend.sql.mgr.AbstractJDBCEnabledManager;
import com.helger.photon.audit.AuditItem;
import com.helger.photon.audit.EAuditActionType;
import com.helger.photon.audit.IAuditActionStringProvider;
import com.helger.photon.audit.IAuditItem;
import com.helger.photon.audit.IAuditor;
import com.helger.security.authentication.subject.user.CUserID;
import com.helger.security.authentication.subject.user.ICurrentUserIDProvider;

/**
 * A special implementation of {@link IAuditor} writing data to a SQL table
 *
 * @author Philip Helger
 */
public class AuditorJDBC extends AbstractJDBCEnabledManager implements IAuditor
{
  public static final int OBJECT_TYPE_MAX_LENGTH = 100;
  private static final Logger LOGGER = LoggerFactory.getLogger (AuditorJDBC.class);

  private final ICurrentUserIDProvider m_aCurrentUserIDProvider;

  /**
   * Constructor
   *
   * @param aDBExecSupplier
   *        The supplier for {@link DBExecutor} objects. May not be
   *        <code>null</code>.
   * @param aCurrentUserIDProvider
   *        The current user ID provider. May not be <code>null</code>.
   */
  public AuditorJDBC (final Supplier <? extends DBExecutor> aDBExecSupplier, @Nonnull final ICurrentUserIDProvider aCurrentUserIDProvider)
  {
    super (aDBExecSupplier);
    m_aCurrentUserIDProvider = ValueEnforcer.notNull (aCurrentUserIDProvider, "UserIDProvider");
  }

  public void createAuditItem (@Nonnull final EAuditActionType eActionType,
                               @Nonnull final ESuccess eSuccess,
                               @Nullable final ObjectType aActionObjectType,
                               @Nullable final String sAction,
                               @Nullable final Object... aArgs)
  {
    // Maybe null, so default like XML version
    final String sUserID = StringHelper.getNotEmpty (m_aCurrentUserIDProvider.getCurrentUserID (), CUserID.USER_ID_GUEST);
    // Combine arguments
    final String sFullAction = IAuditActionStringProvider.JSON.apply (aActionObjectType != null ? aActionObjectType.getName () : sAction,
                                                                      aArgs);

    final DBExecutor aExecutor;
    try
    {
      aExecutor = newExecutor ();
    }
    catch (final IllegalStateException ex)
    {
      // Happens e.g. on shutdown
      return;
    }
    final ESuccess eDBSuccess = aExecutor.performInTransaction ( () -> {
      // Create new
      final long nCreated = aExecutor.insertOrUpdateOrDelete ("INSERT INTO smp_audit (dt, userid, actiontype, success, action)" +
                                                              " VALUES (?, ?, ?, ?, ?)",
                                                              new ConstantPreparedStatementDataProvider (toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                         getTrimmedToLength (sUserID, 20),
                                                                                                         getTrimmedToLength (eActionType.getID (),
                                                                                                                             10),
                                                                                                         Boolean.valueOf (eSuccess.isSuccess ()),
                                                                                                         sFullAction));
      if (nCreated != 1)
      {
        // This may be triggered on the first startup where the smp_audit table
        // is not yet present
        throw new IllegalStateException ("Failed to create new DB entry (" + nCreated + ")");
      }
    });

    if (eDBSuccess.isFailure ())
      LOGGER.error ("Failed to write audit item to DB");
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IAuditItem> getLastAuditItems (@Nonnegative final int nMaxItems)
  {
    ValueEnforcer.isGT0 (nMaxItems, "MaxItems");

    final ICommonsList <IAuditItem> ret = new CommonsArrayList <> ();
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT dt, userid, actiontype, success, action FROM smp_audit" +
                                                                          " ORDER BY dt DESC" +
                                                                          " LIMIT ?",
                                                                          new ConstantPreparedStatementDataProvider (Integer.valueOf (nMaxItems)));
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
      {
        ret.add (new AuditItem (aRow.getAsLocalDateTime (0),
                                aRow.getAsString (1),
                                EAuditActionType.getFromIDOrNull (aRow.getAsString (2)),
                                ESuccess.valueOf (aRow.getAsBoolean (3)),
                                aRow.getAsString (4)));
      }
    return ret;
  }

  @Nullable
  public LocalDate getEarliestAuditDate ()
  {
    final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
    newExecutor ().querySingle ("SELECT dt FROM smp_audit ORDER BY dt ASC LIMIT 1", aDBResult::set);
    if (aDBResult.isSet ())
    {
      // getAsLocalDate does not work
      final LocalDateTime aLDT = aDBResult.get ().getAsLocalDateTime (0);
      if (aLDT != null)
        return aLDT.toLocalDate ();
    }
    return null;
  }
}
