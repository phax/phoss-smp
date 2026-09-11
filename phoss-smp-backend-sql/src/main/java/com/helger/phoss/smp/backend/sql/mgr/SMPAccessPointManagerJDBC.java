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

import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.wrapper.Wrapper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.db.api.helper.DBValueHelper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.db.jdbc.mgr.AbstractJDBCEnabledManager;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPointHelper;
import com.helger.photon.audit.AuditHelper;

/**
 * A JDBC based implementation of the {@link ISMPAccessPointManager} interface.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public final class SMPAccessPointManagerJDBC extends AbstractJDBCEnabledManager implements ISMPAccessPointManager
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
  public SMPAccessPointManagerJDBC (@NonNull final Supplier <? extends DBExecutor> aDBExecSupplier,
                                    @NonNull final String sTableNamePrefix)
  {
    super (aDBExecSupplier);
    ValueEnforcer.notNull (sTableNamePrefix, "TableNamePrefix");
    m_sTableName = sTableNamePrefix + "smp_access_point";
  }

  /**
   * @return The effective table name of the Access Point table. Never <code>null</code>.
   */
  @NonNull
  public String getTableName ()
  {
    return m_sTableName;
  }

  @NonNull
  private static SMPAccessPoint _toDomain (@NonNull final DBResultRow aRow)
  {
    return new SMPAccessPoint (aRow.getAsString (0), aRow.getAsString (1), aRow.getAsString (2));
  }

  @Nullable
  public ISMPAccessPoint findAccessPoint (@Nullable final String sEndpointReference,
                                          @Nullable final String sCertificate)
  {
    // SQL cannot compare CLOBs portably, so pre-select by the URL and compare
    // the certificate in Java. The number of Access Points per URL is small.
    final String sLookupKey = SMPAccessPointHelper.createLookupKey (sEndpointReference, sCertificate);
    final ICommonsList <DBResultRow> aDBResult;
    if (sEndpointReference == null)
      aDBResult = newExecutor ().queryAll ("SELECT id, endpointReference, certificate FROM " +
                                           m_sTableName +
                                           " WHERE endpointReference IS NULL");
    else
      aDBResult = newExecutor ().queryAll ("SELECT id, endpointReference, certificate FROM " +
                                           m_sTableName +
                                           " WHERE endpointReference=?",
                                           new ConstantPreparedStatementDataProvider (sEndpointReference));
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
      {
        final SMPAccessPoint aAP = _toDomain (aRow);
        if (SMPAccessPointHelper.createLookupKey (aAP).equals (sLookupKey))
          return aAP;
      }
    return null;
  }

  @NonNull
  public ISMPAccessPoint getOrCreateAccessPoint (@Nullable final String sEndpointReference,
                                                 @Nullable final String sCertificate)
  {
    final ISMPAccessPoint aExisting = findAccessPoint (sEndpointReference, sCertificate);
    if (aExisting != null)
      return aExisting;

    final SMPAccessPoint aNew = SMPAccessPoint.createDetached (sEndpointReference, sCertificate);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction (() -> {
      final long nCreated = aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                                              m_sTableName +
                                                              " (id, endpointReference, certificate) VALUES (?, ?, ?)",
                                                              new ConstantPreparedStatementDataProvider (DBValueHelper.getTrimmedToLength (aNew.getID (),
                                                                                                                                          CSMPServer.MAX_LEN_ID),
                                                                                                         DBValueHelper.getTrimmedToLength (sEndpointReference,
                                                                                                                                           ENDPOINT_REFERENCE_MAX_LENGTH),
                                                                                                         sCertificate));
      if (nCreated != 1)
        throw new IllegalStateException ("Failed to create new DB entry (" + nCreated + ")");
    });
    if (eSuccess.isFailure ())
      throw new IllegalStateException ("Failed to insert Access Point '" + aNew.getID () + "' into the database");

    AuditHelper.onAuditCreateSuccess (SMPAccessPoint.OT, aNew.getID (), sEndpointReference);
    return aNew;
  }

  @Nullable
  public ISMPAccessPoint getAccessPointOfID (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return null;

    final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
    newExecutor ().querySingle ("SELECT id, endpointReference, certificate FROM " + m_sTableName + " WHERE id=?",
                                new ConstantPreparedStatementDataProvider (sID),
                                aDBResult::set);
    return aDBResult.isSet () ? _toDomain (aDBResult.get ()) : null;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPAccessPoint> getAllAccessPoints ()
  {
    final ICommonsList <ISMPAccessPoint> ret = new CommonsArrayList <> ();
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT id, endpointReference, certificate FROM " +
                                                                          m_sTableName);
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
        ret.add (_toDomain (aRow));
    return ret;
  }

  @Nonnegative
  public long getAccessPointCount ()
  {
    return newExecutor ().queryCount ("SELECT COUNT(*) FROM " + m_sTableName);
  }

  @NonNull
  public EChange deleteAccessPoint (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return EChange.UNCHANGED;

    final long nDeleted = newExecutor ().insertOrUpdateOrDelete ("DELETE FROM " + m_sTableName + " WHERE id=?",
                                                                 new ConstantPreparedStatementDataProvider (sID));
    if (nDeleted <= 0)
    {
      AuditHelper.onAuditDeleteFailure (SMPAccessPoint.OT, sID, "no-such-id");
      return EChange.UNCHANGED;
    }
    AuditHelper.onAuditDeleteSuccess (SMPAccessPoint.OT, sID);
    return EChange.CHANGED;
  }

  @Nonnegative
  public long deleteAllUnusedAccessPoints (@NonNull final ICommonsSet <String> aUsedIDs)
  {
    ValueEnforcer.notNull (aUsedIDs, "UsedIDs");

    long nDeleted = 0;
    for (final ISMPAccessPoint aAP : getAllAccessPoints ())
      if (!aUsedIDs.contains (aAP.getID ()))
        if (deleteAccessPoint (aAP.getID ()).isChanged ())
          nDeleted++;
    return nDeleted;
  }
}
