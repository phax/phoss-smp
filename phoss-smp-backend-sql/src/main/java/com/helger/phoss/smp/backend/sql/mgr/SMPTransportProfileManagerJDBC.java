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
package com.helger.phoss.smp.backend.sql.mgr;

import java.util.function.Supplier;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.numeric.mutable.MutableLong;
import com.helger.base.state.EChange;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.wrapper.Wrapper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.db.api.helper.DBValueHelper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.db.jdbc.mgr.AbstractJDBCEnabledManager;
import com.helger.peppol.smp.ESMPTransportProfileState;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smp.SMPTransportProfile;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.photon.audit.AuditHelper;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Implementation of {@link ISMPTransportProfileManager} for SQL backends.
 *
 * @author Philip Helger
 * @since 5.5.0
 */
public class SMPTransportProfileManagerJDBC extends AbstractJDBCEnabledManager implements ISMPTransportProfileManager
{
  /**
   * Constructor
   *
   * @param aDBExecSupplier
   *        The supplier for {@link DBExecutor} objects. May not be <code>null</code>.
   */
  public SMPTransportProfileManagerJDBC (@Nonnull final Supplier <? extends DBExecutor> aDBExecSupplier)
  {
    super (aDBExecSupplier);
  }

  @Nullable
  public ISMPTransportProfile createSMPTransportProfile (@Nonnull @Nonempty final String sID,
                                                         @Nonnull @Nonempty final String sName,
                                                         final boolean bIsDeprecated)
  {
    final ISMPTransportProfile ret = new SMPTransportProfile (sID,
                                                              sName,
                                                              bIsDeprecated ? ESMPTransportProfileState.DEPRECATED
                                                                            : ESMPTransportProfileState.ACTIVE);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Create new
      final long nCreated = aExecutor.insertOrUpdateOrDelete ("INSERT INTO smp_tprofile (id, name, deprecated) VALUES (?, ?, ?)",
                                                              new ConstantPreparedStatementDataProvider (DBValueHelper.getTrimmedToLength (ret.getID (),
                                                                                                                                           45),
                                                                                                         ret.getName (),
                                                                                                         Boolean.valueOf (ret.getState () ==
                                                                                                                          ESMPTransportProfileState.DEPRECATED)));
      if (nCreated != 1)
        throw new IllegalStateException ("Failed to create new DB entry (" + nCreated + ")");
    });

    if (eSuccess.isFailure ())
    {
      AuditHelper.onAuditCreateFailure (SMPTransportProfile.OT,
                                        sID,
                                        sName,
                                        Boolean.valueOf (bIsDeprecated),
                                        "database-error");
      return null;
    }

    AuditHelper.onAuditCreateSuccess (SMPTransportProfile.OT, sID, sName, Boolean.valueOf (bIsDeprecated));
    return ret;
  }

  @Nonnull
  public EChange updateSMPTransportProfile (@Nullable final String sSMPTransportProfileID,
                                            @Nonnull @Nonempty final String sName,
                                            final boolean bIsDeprecated)
  {
    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_tprofile SET name=?, deprecated=? WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (sName,
                                                                                                         Boolean.valueOf (bIsDeprecated),
                                                                                                         sSMPTransportProfileID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (SMPTransportProfile.OT, "update", sSMPTransportProfileID, "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such transport profile ID
      AuditHelper.onAuditModifyFailure (SMPTransportProfile.OT, "update", sSMPTransportProfileID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (SMPTransportProfile.OT,
                                      "update",
                                      sSMPTransportProfileID,
                                      sName,
                                      Boolean.valueOf (bIsDeprecated));
    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteSMPTransportProfile (@Nullable final String sSMPTransportProfileID)
  {
    if (StringHelper.isEmpty (sSMPTransportProfileID))
      return EChange.UNCHANGED;

    final long nDeleted = newExecutor ().insertOrUpdateOrDelete ("DELETE FROM smp_tprofile WHERE id=?",
                                                                 new ConstantPreparedStatementDataProvider (sSMPTransportProfileID));
    if (nDeleted == 0)
    {
      AuditHelper.onAuditDeleteFailure (SMPTransportProfile.OT, sSMPTransportProfileID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditDeleteSuccess (SMPTransportProfile.OT, sSMPTransportProfileID);
    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPTransportProfile> getAllSMPTransportProfiles ()
  {
    final ICommonsList <ISMPTransportProfile> ret = new CommonsArrayList <> ();
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT id, name, deprecated FROM smp_tprofile");
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
      {
        ret.add (new SMPTransportProfile (aRow.getAsString (0),
                                          aRow.getAsString (1),
                                          aRow.getAsBoolean (2, false) ? ESMPTransportProfileState.DEPRECATED
                                                                       : ESMPTransportProfileState.ACTIVE));
      }
    return ret;
  }

  @Nullable
  public ISMPTransportProfile getSMPTransportProfileOfID (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return null;

    final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
    newExecutor ().querySingle ("SELECT name, deprecated FROM smp_tprofile WHERE id=?",
                                new ConstantPreparedStatementDataProvider (sID),
                                aDBResult::set);
    if (aDBResult.isNotSet ())
      return null;

    final DBResultRow aRow = aDBResult.get ();
    return new SMPTransportProfile (sID,
                                    aRow.getAsString (0),
                                    aRow.getAsBoolean (1, false) ? ESMPTransportProfileState.DEPRECATED
                                                                 : ESMPTransportProfileState.ACTIVE);
  }

  public boolean containsSMPTransportProfileWithID (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return false;

    return newExecutor ().queryCount ("SELECT COUNT(*) FROM smp_tprofile WHERE id=?",
                                      new ConstantPreparedStatementDataProvider (sID)) > 0;
  }

  @Nonnegative
  public long getSMPTransportProfileCount ()
  {
    return newExecutor ().queryCount ("SELECT COUNT(*) FROM smp_tprofile");
  }
}
