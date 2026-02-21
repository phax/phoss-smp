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

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.numeric.mutable.MutableLong;
import com.helger.base.state.EChange;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.wrapper.Wrapper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.db.jdbc.mgr.AbstractJDBCEnabledManager;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.sml.SMLInfo;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.photon.audit.AuditHelper;

/**
 * A JDBC based implementation of the {@link ISMLInfoManager} interface.
 *
 * @author Philip Helger
 */
public final class SMLInfoManagerJDBC extends AbstractJDBCEnabledManager implements ISMLInfoManager
{
  private static final boolean DEFAULT_CLIENT_CERT_REQUIRED = false;
  private final String m_sTableName;

  /**
   * Constructor
   *
   * @param aDBExecSupplier
   *        The supplier for {@link DBExecutor} objects. May not be <code>null</code>.
   * @param sTableNamePrefix
   *        The table name prefix to be used. May not be <code>null</code>.
   */
  public SMLInfoManagerJDBC (@NonNull final Supplier <? extends DBExecutor> aDBExecSupplier,
                             @NonNull final String sTableNamePrefix)
  {
    super (aDBExecSupplier);
    m_sTableName = sTableNamePrefix + "smp_sml_info";
  }

  @NonNull
  public ISMLInfo createSMLInfo (@NonNull @Nonempty final String sDisplayName,
                                 @NonNull @Nonempty final String sDNSZone,
                                 @NonNull @Nonempty final String sManagementServiceURL,
                                 @NonNull final String sURLSuffixManageSMP,
                                 @NonNull final String sURLSuffixManageParticipant,
                                 final boolean bClientCertificateRequired)
  {
    final SMLInfo aSMLInfo = SMLInfo.builder ()
                                    .idNewPersistent ()
                                    .displayName (sDisplayName)
                                    .dnsZone (sDNSZone)
                                    .managementServiceURL (sManagementServiceURL)
                                    .urlSuffixManageSMP (sURLSuffixManageSMP)
                                    .urlSuffixManageParticipant (sURLSuffixManageParticipant)
                                    .clientCertificateRequired (bClientCertificateRequired)
                                    .build ();

    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      final long nCreated = aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                                              m_sTableName +
                                                              " (id, displayname, dnszone, serviceurl, managesmp, manageparticipant, clientcert)" +
                                                              " VALUES (?, ?, ?, ?, ?, ?, ?)",
                                                              new ConstantPreparedStatementDataProvider (aSMLInfo.getID (),
                                                                                                         sDisplayName,
                                                                                                         sDNSZone,
                                                                                                         sManagementServiceURL,
                                                                                                         sURLSuffixManageSMP,
                                                                                                         sURLSuffixManageParticipant,
                                                                                                         Boolean.valueOf (bClientCertificateRequired)));
      if (nCreated != 1)
        throw new IllegalStateException ("Failed to create new DB entry (" + nCreated + ")");
    });

    if (eSuccess.isFailure ())
      throw new IllegalStateException ("Failed to insert SMLInfo '" + aSMLInfo.getID () + "' into the database");

    AuditHelper.onAuditCreateSuccess (SMLInfo.OT,
                                      aSMLInfo.getID (),
                                      sDisplayName,
                                      sDNSZone,
                                      sManagementServiceURL,
                                      sURLSuffixManageSMP,
                                      sURLSuffixManageParticipant,
                                      Boolean.valueOf (bClientCertificateRequired));
    return aSMLInfo;
  }

  @NonNull
  public EChange updateSMLInfo (@NonNull final String sSMLInfoID,
                                @NonNull @Nonempty final String sDisplayName,
                                @NonNull @Nonempty final String sDNSZone,
                                @NonNull @Nonempty final String sManagementServiceURL,
                                @NonNull final String sURLSuffixManageSMP,
                                @NonNull final String sURLSuffixManageParticipant,
                                final boolean bClientCertificateRequired)
  {
    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE " +
                                                              m_sTableName +
                                                              " SET displayname=?, dnszone=?, serviceurl=?, managesmp=?, manageparticipant=?, clientcert=?" +
                                                              " WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (sDisplayName,
                                                                                                         sDNSZone,
                                                                                                         sManagementServiceURL,
                                                                                                         sURLSuffixManageSMP,
                                                                                                         sURLSuffixManageParticipant,
                                                                                                         Boolean.valueOf (bClientCertificateRequired),
                                                                                                         sSMLInfoID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      AuditHelper.onAuditModifyFailure (SMLInfo.OT, "set-all", sSMLInfoID, "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      AuditHelper.onAuditModifyFailure (SMLInfo.OT, "set-all", sSMLInfoID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (SMLInfo.OT,
                                      "set-all",
                                      sSMLInfoID,
                                      sDisplayName,
                                      sDNSZone,
                                      sManagementServiceURL,
                                      sURLSuffixManageSMP,
                                      sURLSuffixManageParticipant,
                                      Boolean.valueOf (bClientCertificateRequired));
    return EChange.CHANGED;
  }

  @Nullable
  public EChange deleteSMLInfo (@Nullable final String sSMLInfoID)
  {
    if (StringHelper.isEmpty (sSMLInfoID))
      return EChange.UNCHANGED;

    final long nDeleted = newExecutor ().insertOrUpdateOrDelete ("DELETE FROM " + m_sTableName + " WHERE id=?",
                                                                 new ConstantPreparedStatementDataProvider (sSMLInfoID));
    if (nDeleted == 0)
    {
      AuditHelper.onAuditDeleteFailure (SMLInfo.OT, sSMLInfoID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditDeleteSuccess (SMLInfo.OT, sSMLInfoID);
    return EChange.CHANGED;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMLInfo> getAllSMLInfos ()
  {
    final ICommonsList <ISMLInfo> ret = new CommonsArrayList <> ();
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT id, displayname, dnszone, serviceurl, managesmp, manageparticipant, clientcert" +
                                                                          " FROM " +
                                                                          m_sTableName);
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
        ret.add (SMLInfo.builder ()
                        .id (aRow.getAsString (0))
                        .displayName (aRow.getAsString (1))
                        .dnsZone (aRow.getAsString (2))
                        .managementServiceURL (aRow.getAsString (3))
                        .urlSuffixManageSMP (aRow.getAsString (4))
                        .urlSuffixManageParticipant (aRow.getAsString (5))
                        .clientCertificateRequired (aRow.getAsBoolean (6, DEFAULT_CLIENT_CERT_REQUIRED))
                        .build ());
    return ret;
  }

  @Nullable
  public ISMLInfo getSMLInfoOfID (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return null;

    final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
    newExecutor ().querySingle ("SELECT displayname, dnszone, serviceurl, managesmp, manageparticipant, clientcert" +
                                " FROM " +
                                m_sTableName +
                                " WHERE id=?",
                                new ConstantPreparedStatementDataProvider (sID),
                                aDBResult::set);
    if (aDBResult.isNotSet ())
      return null;

    final DBResultRow aRow = aDBResult.get ();
    return SMLInfo.builder ()
                  .id (sID)
                  .displayName (aRow.getAsString (0))
                  .dnsZone (aRow.getAsString (1))
                  .managementServiceURL (aRow.getAsString (2))
                  .urlSuffixManageSMP (aRow.getAsString (3))
                  .urlSuffixManageParticipant (aRow.getAsString (4))
                  .clientCertificateRequired (aRow.getAsBoolean (5, DEFAULT_CLIENT_CERT_REQUIRED))
                  .build ();
  }

  public boolean containsSMLInfoWithID (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return false;

    return newExecutor ().queryCount ("SELECT COUNT(*) FROM " + m_sTableName + " WHERE id=?",
                                      new ConstantPreparedStatementDataProvider (sID)) > 0;
  }
}
