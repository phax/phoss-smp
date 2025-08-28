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
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
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
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationDirection;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationState;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigration;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.pmigration.SMPParticipantMigration;
import com.helger.photon.audit.AuditHelper;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Implementation of {@link ISMPParticipantMigrationManager} for JDBC
 *
 * @author Philip Helger
 * @since 5.4.0
 */
public class SMPParticipantMigrationManagerJDBC extends AbstractJDBCEnabledManager implements
                                                ISMPParticipantMigrationManager
{
  /**
   * Constructor
   *
   * @param aDBExecSupplier
   *        The supplier for {@link DBExecutor} objects. May not be <code>null</code>.
   */
  public SMPParticipantMigrationManagerJDBC (@Nonnull final Supplier <? extends DBExecutor> aDBExecSupplier)
  {
    super (aDBExecSupplier);
  }

  @Nullable
  private ISMPParticipantMigration _createParticipantMigration (@Nonnull final SMPParticipantMigration aSMPParticipantMigration)
  {
    ValueEnforcer.notNull (aSMPParticipantMigration, "SMPParticipantMigration");

    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Create new
      final long nCreated = aExecutor.insertOrUpdateOrDelete ("INSERT INTO smp_pmigration (id, direction, state, pid, initdt, migkey)" +
                                                              " VALUES (?, ?, ?, ?, ?, ?)",
                                                              new ConstantPreparedStatementDataProvider (aSMPParticipantMigration.getID (),
                                                                                                         aSMPParticipantMigration.getDirection ()
                                                                                                                                 .getID (),
                                                                                                         aSMPParticipantMigration.getState ()
                                                                                                                                 .getID (),
                                                                                                         aSMPParticipantMigration.getParticipantIdentifier ()
                                                                                                                                 .getURIEncoded (),
                                                                                                         DBValueHelper.toTimestamp (aSMPParticipantMigration.getInitiationDateTime ()),
                                                                                                         aSMPParticipantMigration.getMigrationKey ()));
      if (nCreated != 1)
        throw new IllegalStateException ("Failed to create new DB entry (" + nCreated + ")");
    });

    if (eSuccess.isFailure ())
    {
      AuditHelper.onAuditCreateFailure (SMPParticipantMigration.OT,
                                        aSMPParticipantMigration.getID (),
                                        "database-error");
      return null;
    }

    AuditHelper.onAuditCreateSuccess (SMPParticipantMigration.OT,
                                      aSMPParticipantMigration.getID (),
                                      aSMPParticipantMigration.getDirection (),
                                      aSMPParticipantMigration.getParticipantIdentifier ().getURIEncoded (),
                                      aSMPParticipantMigration.getInitiationDateTime (),
                                      aSMPParticipantMigration.getMigrationKey ());
    return aSMPParticipantMigration;
  }

  @Nullable
  public ISMPParticipantMigration createOutboundParticipantMigration (@Nonnull final IParticipantIdentifier aParticipantID,
                                                                      @Nonnull @Nonempty final String sMigrationKey)
  {
    final SMPParticipantMigration aSMPParticipantMigration = SMPParticipantMigration.createOutbound (aParticipantID,
                                                                                                     sMigrationKey);
    return _createParticipantMigration (aSMPParticipantMigration);
  }

  @Nullable
  public ISMPParticipantMigration createInboundParticipantMigration (@Nonnull final IParticipantIdentifier aParticipantID,
                                                                     @Nonnull @Nonempty final String sMigrationKey)
  {
    final SMPParticipantMigration aSMPParticipantMigration = SMPParticipantMigration.createInbound (aParticipantID,
                                                                                                    sMigrationKey);
    return _createParticipantMigration (aSMPParticipantMigration);
  }

  @Nonnull
  public EChange deleteParticipantMigrationOfID (@Nullable final String sParticipantMigrationID)
  {
    if (StringHelper.isEmpty (sParticipantMigrationID))
      return EChange.UNCHANGED;

    final long nDeleted = newExecutor ().insertOrUpdateOrDelete ("DELETE FROM smp_pmigration" + " WHERE id=?",
                                                                 new ConstantPreparedStatementDataProvider (sParticipantMigrationID));
    if (nDeleted == 0)
    {
      AuditHelper.onAuditDeleteFailure (SMPParticipantMigration.OT, sParticipantMigrationID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditDeleteSuccess (SMPParticipantMigration.OT, sParticipantMigrationID);
    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteAllParticipantMigrationsOfParticipant (@Nonnull final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");

    final long nDeleted = newExecutor ().insertOrUpdateOrDelete ("DELETE FROM smp_pmigration" + " WHERE pid=?",
                                                                 new ConstantPreparedStatementDataProvider (aParticipantID.getURIEncoded ()));
    if (nDeleted == 0)
    {
      AuditHelper.onAuditDeleteFailure (SMPParticipantMigration.OT,
                                        aParticipantID.getURIEncoded (),
                                        "no-such-participant-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditDeleteSuccess (SMPParticipantMigration.OT, aParticipantID.getURIEncoded ());
    return EChange.CHANGED;
  }

  @Nonnull
  public EChange setParticipantMigrationState (@Nullable final String sParticipantMigrationID,
                                               @Nonnull final EParticipantMigrationState eNewState)
  {
    ValueEnforcer.notNull (eNewState, "NewState");

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_pmigration SET state=? WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (eNewState.getID (),
                                                                                                         sParticipantMigrationID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (SMPParticipantMigration.OT,
                                        "set-migration-state",
                                        sParticipantMigrationID,
                                        eNewState,
                                        "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such participant migration ID
      AuditHelper.onAuditModifyFailure (SMPParticipantMigration.OT,
                                        "set-migration-state",
                                        sParticipantMigrationID,
                                        "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (SMPParticipantMigration.OT,
                                      "set-migration-state",
                                      sParticipantMigrationID,
                                      eNewState);
    return EChange.CHANGED;
  }

  @Nullable
  public SMPParticipantMigration getParticipantMigrationOfID (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return null;

    final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
    newExecutor ().querySingle ("SELECT direction, state, pid, initdt, migkey FROM smp_pmigration WHERE id=?",
                                new ConstantPreparedStatementDataProvider (sID),
                                aDBResult::set);
    if (aDBResult.isNotSet ())
      return null;

    final DBResultRow aRow = aDBResult.get ();
    final EParticipantMigrationDirection eDirection = EParticipantMigrationDirection.getFromIDOrNull (aRow.getAsString (0));
    final EParticipantMigrationState eState = EParticipantMigrationState.getFromIDOrNull (aRow.getAsString (1));
    final IParticipantIdentifier aPI = SMPMetaManager.getIdentifierFactory ()
                                                     .parseParticipantIdentifier (aRow.getAsString (2));
    return new SMPParticipantMigration (sID,
                                        eDirection,
                                        eState,
                                        aPI,
                                        aRow.getAsLocalDateTime (3),
                                        aRow.getAsString (4));
  }

  @Nullable
  public ISMPParticipantMigration getParticipantMigrationOfParticipantID (@Nonnull final EParticipantMigrationDirection eDirection,
                                                                          @Nonnull final EParticipantMigrationState eState,
                                                                          @Nullable final IParticipantIdentifier aParticipantID)
  {
    ValueEnforcer.notNull (eDirection, "Direction");
    ValueEnforcer.notNull (eState, "State");
    if (aParticipantID == null)
      return null;

    final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
    newExecutor ().querySingle ("SELECT id, initdt, migkey FROM smp_pmigration WHERE direction=? AND state=? AND pid=?",
                                new ConstantPreparedStatementDataProvider (eDirection.getID (),
                                                                           eState.getID (),
                                                                           aParticipantID.getURIEncoded ()),
                                aDBResult::set);
    if (aDBResult.isNotSet ())
      return null;

    final DBResultRow aRow = aDBResult.get ();
    return new SMPParticipantMigration (aRow.getAsString (0),
                                        eDirection,
                                        eState,
                                        aParticipantID,
                                        aRow.getAsLocalDateTime (1),
                                        aRow.getAsString (2));
  }

  @Nonnull
  @ReturnsMutableCopy
  private ICommonsList <ISMPParticipantMigration> _getAllParticipantMigrations (@Nonnull final EParticipantMigrationDirection eDirection,
                                                                                @Nullable final EParticipantMigrationState eState)
  {
    final ICommonsList <ISMPParticipantMigration> ret = new CommonsArrayList <> ();
    final ICommonsList <DBResultRow> aDBResult;
    if (eState == null)
    {
      // Use all states
      aDBResult = newExecutor ().queryAll ("SELECT id, state, pid, initdt, migkey FROM smp_pmigration WHERE direction=?",
                                           new ConstantPreparedStatementDataProvider (eDirection.getID ()));
      if (aDBResult != null)
        for (final DBResultRow aRow : aDBResult)
        {
          final EParticipantMigrationState eRealState = EParticipantMigrationState.getFromIDOrNull (aRow.getAsString (1));
          final IParticipantIdentifier aPI = SMPMetaManager.getIdentifierFactory ()
                                                           .parseParticipantIdentifier (aRow.getAsString (2));
          ret.add (new SMPParticipantMigration (aRow.getAsString (0),
                                                eDirection,
                                                eRealState,
                                                aPI,
                                                aRow.getAsLocalDateTime (3),
                                                aRow.getAsString (4)));
        }
    }
    else
    {
      // Use specific state
      aDBResult = newExecutor ().queryAll ("SELECT id, pid, initdt, migkey FROM smp_pmigration WHERE direction=? AND state=?",
                                           new ConstantPreparedStatementDataProvider (eDirection.getID (),
                                                                                      eState.getID ()));
      if (aDBResult != null)
        for (final DBResultRow aRow : aDBResult)
        {
          final IParticipantIdentifier aPI = SMPMetaManager.getIdentifierFactory ()
                                                           .parseParticipantIdentifier (aRow.getAsString (1));
          ret.add (new SMPParticipantMigration (aRow.getAsString (0),
                                                eDirection,
                                                eState,
                                                aPI,
                                                aRow.getAsLocalDateTime (2),
                                                aRow.getAsString (3)));
        }
    }
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPParticipantMigration> getAllOutboundParticipantMigrations (@Nullable final EParticipantMigrationState eState)
  {
    return _getAllParticipantMigrations (EParticipantMigrationDirection.OUTBOUND, eState);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPParticipantMigration> getAllInboundParticipantMigrations (@Nullable final EParticipantMigrationState eState)
  {
    return _getAllParticipantMigrations (EParticipantMigrationDirection.INBOUND, eState);
  }

  private boolean _containsMigration (@Nonnull final EParticipantMigrationDirection eDirection,
                                      @Nonnull final EParticipantMigrationState eState,
                                      @Nullable final IParticipantIdentifier aParticipantID)
  {
    if (aParticipantID == null)
      return false;

    return newExecutor ().queryCount ("SELECT COUNT(*) FROM smp_pmigration WHERE direction=? AND state=? AND pid=?",
                                      new ConstantPreparedStatementDataProvider (eDirection.getID (),
                                                                                 eState.getID (),
                                                                                 aParticipantID.getURIEncoded ())) > 0;
  }

  public boolean containsOutboundMigrationInProgress (@Nullable final IParticipantIdentifier aParticipantID)
  {
    return _containsMigration (EParticipantMigrationDirection.OUTBOUND,
                               EParticipantMigrationState.IN_PROGRESS,
                               aParticipantID);
  }

  public boolean containsInboundMigration (@Nullable final IParticipantIdentifier aParticipantID)
  {
    return _containsMigration (EParticipantMigrationDirection.INBOUND,
                               EParticipantMigrationState.MIGRATED,
                               aParticipantID);
  }
}
