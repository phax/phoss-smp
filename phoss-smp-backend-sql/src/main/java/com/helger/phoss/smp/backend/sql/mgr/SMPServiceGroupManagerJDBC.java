/*
 * Copyright (C) 2019-2024 Philip Helger and contributors
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

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.mutable.MutableBoolean;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.wrapper.Wrapper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.db.jdbc.mgr.AbstractJDBCEnabledManager;
import com.helger.peppolid.CIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupCallback;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.servicegroup.SMPServiceGroup;
import com.helger.phoss.smp.exception.SMPInternalErrorException;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPSMLException;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.smlhook.IRegistrationHook;
import com.helger.phoss.smp.smlhook.RegistrationHookException;
import com.helger.phoss.smp.smlhook.RegistrationHookFactory;
import com.helger.photon.audit.AuditHelper;

import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

/**
 * A JDBC based implementation of the {@link ISMPServiceGroupManager} interface.
 *
 * @author Philip Helger
 * @since 5.3.0
 */
public final class SMPServiceGroupManagerJDBC extends AbstractJDBCEnabledManager implements ISMPServiceGroupManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPServiceGroupManagerJDBC.class);

  private final CallbackList <ISMPServiceGroupCallback> m_aCBs = new CallbackList <> ();

  private ExpiringMap <String, SMPServiceGroup> m_aCache;

  /**
   * Constructor
   *
   * @param aDBExecSupplier
   *        The supplier for {@link DBExecutor} objects. May not be
   *        <code>null</code>.
   */
  public SMPServiceGroupManagerJDBC (@Nonnull final Supplier <? extends DBExecutor> aDBExecSupplier)
  {
    super (aDBExecSupplier);
  }

  public boolean isCacheEnabled ()
  {
    return m_aCache != null;
  }

  public void setCacheEnabled (final boolean bEnabled)
  {
    if (bEnabled)
      m_aCache = ExpiringMap.builder ()
                            .expiration (60, TimeUnit.SECONDS)
                            .expirationPolicy (ExpirationPolicy.CREATED)
                            .build ();
    else
      m_aCache = null;
  }

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <ISMPServiceGroupCallback> serviceGroupCallbacks ()
  {
    return m_aCBs;
  }

  @Nonnull
  public SMPServiceGroup createSMPServiceGroup (@Nonnull @Nonempty final String sOwnerID,
                                                @Nonnull final IParticipantIdentifier aParticipantID,
                                                @Nullable final String sExtension,
                                                final boolean bCreateInSML) throws SMPServerException
  {
    ValueEnforcer.notEmpty (sOwnerID, "OwnerID");
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createSMPServiceGroup (" +
                    sOwnerID +
                    ", " +
                    aParticipantID.getURIEncoded () +
                    ", " +
                    (StringHelper.hasText (sExtension) ? "with extension" : "without extension") +
                    ", " +
                    bCreateInSML +
                    ")");

    final MutableBoolean aCreatedSGHook = new MutableBoolean (false);
    final MutableBoolean aCreatedSGDB = new MutableBoolean (false);
    final IRegistrationHook aHook = RegistrationHookFactory.getInstance ();
    final Wrapper <Exception> aCaughtException = new Wrapper <> ();

    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Check if the passed service group ID is already in use
      final SMPServiceGroup aDBServiceGroup = getSMPServiceGroupOfID (aParticipantID);
      if (aDBServiceGroup != null)
        throw new IllegalStateException ("The service group with ID " +
                                         aParticipantID.getURIEncoded () +
                                         " already exists!");

      if (bCreateInSML)
      {
        // It's a new service group - Create in SML and remember that
        // Throws exception in case of an error
        aHook.createServiceGroup (aParticipantID);
        aCreatedSGHook.set (true);
      }

      // Did not exist. Create it.
      if (aExecutor.insertOrUpdateOrDelete ("INSERT INTO smp_service_group (businessIdentifierScheme, businessIdentifier, extension) VALUES (?, ?, ?)",
                                            new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                                       aParticipantID.getValue (),
                                                                                       sExtension)) > 0)
      {
        aCreatedSGDB.set (true);
        aExecutor.insertOrUpdateOrDelete ("INSERT INTO smp_ownership (businessIdentifierScheme, businessIdentifier, username) VALUES (?, ?, ?)",
                                          new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                                     aParticipantID.getValue (),
                                                                                     sOwnerID));
      }
    }, aCaughtException::set);

    if (aCreatedSGHook.booleanValue () && !aCreatedSGDB.booleanValue ())
    {
      // Not created in the DB
      // Undo creation in SML again
      try
      {
        aHook.undoCreateServiceGroup (aParticipantID);
      }
      catch (final RegistrationHookException ex)
      {
        LOGGER.error ("Failed to undoCreateServiceGroup (" + aParticipantID.getURIEncoded () + ")", ex);
      }
    }

    if (eSuccess.isFailure () || aCaughtException.isSet () || !aCreatedSGDB.booleanValue ())
    {
      AuditHelper.onAuditCreateFailure (SMPServiceGroup.OT,
                                        aParticipantID.getURIEncoded (),
                                        sOwnerID,
                                        sExtension,
                                        Boolean.valueOf (bCreateInSML));

      // Propagate contained exception
      final Exception ex = aCaughtException.get ();
      if (ex instanceof SMPServerException)
        throw (SMPServerException) ex;
      if (ex instanceof RegistrationHookException)
        throw new SMPSMLException ("Failed to create '" + aParticipantID.getURIEncoded () + "' in SML", ex);
      throw new SMPInternalErrorException ("Error creating ServiceGroup '" + aParticipantID.getURIEncoded () + "'", ex);
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createSMPServiceGroup succeeded");

    AuditHelper.onAuditCreateSuccess (SMPServiceGroup.OT,
                                      aParticipantID.getURIEncoded (),
                                      sOwnerID,
                                      sExtension,
                                      Boolean.valueOf (bCreateInSML));

    final SMPServiceGroup aServiceGroup = new SMPServiceGroup (sOwnerID, aParticipantID, sExtension);
    if (m_aCache != null)
      m_aCache.put (aParticipantID.getURIEncoded (), aServiceGroup);

    m_aCBs.forEach (x -> x.onSMPServiceGroupCreated (aServiceGroup, bCreateInSML));
    return aServiceGroup;
  }

  @Nonnull
  public EChange updateSMPServiceGroup (@Nonnull final IParticipantIdentifier aParticipantID,
                                        @Nonnull @Nonempty final String sNewOwnerID,
                                        @Nullable final String sNewExtension) throws SMPServerException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notEmpty (sNewOwnerID, "NewOwnerID");
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("updateSMPServiceGroup (" +
                    aParticipantID.getURIEncoded () +
                    ", " +
                    sNewOwnerID +
                    ", " +
                    (StringHelper.hasText (sNewExtension) ? "with extension" : "without extension") +
                    ")");

    final Wrapper <EChange> aWrappedChange = new Wrapper <> (EChange.UNCHANGED);
    final Wrapper <Exception> aCaughtException = new Wrapper <> ();

    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Check if the passed service group ID is already in use
      final SMPServiceGroup aDBServiceGroup = getSMPServiceGroupOfID (aParticipantID);
      if (aDBServiceGroup == null)
        throw new SMPNotFoundException ("The service group with ID " +
                                        aParticipantID.getURIEncoded () +
                                        " does not exist!");

      if (!EqualsHelper.equals (sNewOwnerID, aDBServiceGroup.getOwnerID ()))
      {
        // Update ownership
        final long nCount = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_ownership SET username=? WHERE businessIdentifierScheme=? AND businessIdentifier=?",
                                                              new ConstantPreparedStatementDataProvider (sNewOwnerID,
                                                                                                         aDBServiceGroup.getParticipantIdentifier ()
                                                                                                                        .getScheme (),
                                                                                                         aDBServiceGroup.getParticipantIdentifier ()
                                                                                                                        .getValue ()));
        if (nCount != 1)
          throw new IllegalStateException ("Failed to update the ownership username to '" + sNewOwnerID + "'");
        aWrappedChange.set (EChange.CHANGED);
      }

      if (!EqualsHelper.equals (sNewExtension, aDBServiceGroup.getExtensions ().getExtensionsAsJsonString ()))
      {
        // Update extension
        final long nCount = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_service_group SET extension=? WHERE businessIdentifierScheme=? AND businessIdentifier=?",
                                                              new ConstantPreparedStatementDataProvider (sNewExtension,
                                                                                                         aDBServiceGroup.getParticipantIdentifier ()
                                                                                                                        .getScheme (),
                                                                                                         aDBServiceGroup.getParticipantIdentifier ()
                                                                                                                        .getValue ()));
        if (nCount != 1)
          throw new IllegalStateException ("Failed to update the service_group extension to '" + sNewExtension + "'");
        aWrappedChange.set (EChange.CHANGED);
      }
    }, aCaughtException::set);

    if (eSuccess.isFailure () || aCaughtException.isSet ())
    {
      AuditHelper.onAuditModifyFailure (SMPServiceGroup.OT,
                                        "set-all",
                                        aParticipantID.getURIEncoded (),
                                        sNewOwnerID,
                                        sNewExtension);

      final Exception ex = aCaughtException.get ();
      if (ex instanceof SMPServerException)
        throw (SMPServerException) ex;
      throw new SMPInternalErrorException ("Failed to update ServiceGroup '" + aParticipantID.getURIEncoded () + "'",
                                           ex);
    }

    final EChange eChange = aWrappedChange.get ();
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("updateSMPServiceGroup succeeded. Change=" + eChange.isChanged ());

    AuditHelper.onAuditModifySuccess (SMPServiceGroup.OT,
                                      "set-all",
                                      aParticipantID.getURIEncoded (),
                                      sNewOwnerID,
                                      sNewExtension);

    // Callback only if something changed
    if (eChange.isChanged ())
      m_aCBs.forEach (x -> x.onSMPServiceGroupUpdated (aParticipantID));

    return eChange;
  }

  @Nonnull
  public EChange deleteSMPServiceGroup (@Nonnull final IParticipantIdentifier aParticipantID,
                                        final boolean bDeleteInSML) throws SMPServerException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceGroup (" + aParticipantID.getURIEncoded () + ")");

    final IRegistrationHook aHook = RegistrationHookFactory.getInstance ();
    final MutableBoolean aDeletedServiceGroupInSML = new MutableBoolean (false);
    final Wrapper <EChange> aWrappedChange = new Wrapper <> (EChange.UNCHANGED);
    final Wrapper <Exception> aCaughtException = new Wrapper <> ();

    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Check if the passed service group ID is already in use
      final SMPServiceGroup aDBServiceGroup = getSMPServiceGroupOfID (aParticipantID);
      if (aDBServiceGroup == null)
        throw new SMPNotFoundException ("The service group with ID " +
                                        aParticipantID.getURIEncoded () +
                                        " does not exist!");

      if (bDeleteInSML)
      {
        // Delete in SML - and remember that
        // throws exception in case of error
        aHook.deleteServiceGroup (aParticipantID);
        aDeletedServiceGroupInSML.set (true);
      }

      final long nCount = aExecutor.insertOrUpdateOrDelete ("DELETE FROM smp_service_group" +
                                                            " WHERE businessIdentifierScheme=? AND businessIdentifier=?",
                                                            new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                                                       aParticipantID.getValue ()));
      if (nCount != 1)
        throw new IllegalStateException ("Failed to delete service group");
      aWrappedChange.set (EChange.CHANGED);
    }, aCaughtException::set);

    if (eSuccess.isFailure ())
    {
      // Error writing to the DB
      if (bDeleteInSML && aDeletedServiceGroupInSML.booleanValue ())
      {
        // Undo deletion in SML!
        try
        {
          aHook.undoDeleteServiceGroup (aParticipantID);
        }
        catch (final RegistrationHookException ex)
        {
          LOGGER.error ("Failed to undoDeleteServiceGroup (" + aParticipantID.getURIEncoded () + ")", ex);
        }
      }
    }

    if (aCaughtException.isSet ())
    {
      AuditHelper.onAuditDeleteFailure (SMPServiceGroup.OT, aParticipantID.getURIEncoded (), "database-error");

      final Exception ex = aCaughtException.get ();
      if (ex instanceof SMPServerException)
        throw (SMPServerException) ex;
      if (ex instanceof RegistrationHookException)
        throw new SMPSMLException ("Failed to delete '" + aParticipantID.getURIEncoded () + "' in SML", ex);
      throw new SMPInternalErrorException ("Failed to delete ServiceGroup '" + aParticipantID.getURIEncoded () + "'",
                                           ex);
    }

    final EChange eChange = aWrappedChange.get ();
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceGroup succeeded. Change=" + eChange.isChanged ());

    if (eChange.isChanged ())
    {
      AuditHelper.onAuditDeleteSuccess (SMPServiceGroup.OT, aParticipantID.getURIEncoded ());

      if (m_aCache != null)
        m_aCache.remove (aParticipantID.getURIEncoded ());
      m_aCBs.forEach (x -> x.onSMPServiceGroupDeleted (aParticipantID, bDeleteInSML));
    }

    return eChange;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroups ()
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("getAllSMPServiceGroups()");

    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT sg.businessIdentifierScheme, sg.businessIdentifier, sg.extension, so.username" +
                                                                          " FROM smp_service_group sg, smp_ownership so" +
                                                                          " WHERE so.businessIdentifierScheme=sg.businessIdentifierScheme AND so.businessIdentifier=sg.businessIdentifier");

    final ICommonsList <ISMPServiceGroup> ret = new CommonsArrayList <> ();
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
        ret.add (new SMPServiceGroup (aRow.getAsString (3),
                                      new SimpleParticipantIdentifier (aRow.getAsString (0), aRow.getAsString (1)),
                                      aRow.getAsString (2)));
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsSet <String> getAllSMPServiceGroupIDs ()
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("getAllSMPServiceGroupIDs()");

    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT sg.businessIdentifierScheme, sg.businessIdentifier" +
                                                                          " FROM smp_service_group sg");

    final ICommonsSet <String> ret = new CommonsHashSet <> ();
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
        ret.add (CIdentifier.getURIEncoded (aRow.getAsString (0), aRow.getAsString (1)));
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroupsOfOwner (@Nonnull final String sOwnerID)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("getAllSMPServiceGroupsOfOwner(" + sOwnerID + ")");

    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT sg.businessIdentifierScheme, sg.businessIdentifier, sg.extension" +
                                                                          " FROM smp_service_group sg, smp_ownership so" +
                                                                          " WHERE so.username=?" +
                                                                          " AND so.businessIdentifierScheme=sg.businessIdentifierScheme AND so.businessIdentifier=sg.businessIdentifier",
                                                                          new ConstantPreparedStatementDataProvider (sOwnerID));

    final ICommonsList <ISMPServiceGroup> ret = new CommonsArrayList <> ();
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
        ret.add (new SMPServiceGroup (sOwnerID,
                                      new SimpleParticipantIdentifier (aRow.getAsString (0), aRow.getAsString (1)),
                                      aRow.getAsString (2)));
    return ret;
  }

  @Nonnegative
  public long getSMPServiceGroupCountOfOwner (@Nonnull final String sOwnerID)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("getSMPServiceGroupCountOfOwner(" + sOwnerID + ")");

    return newExecutor ().queryCount ("SELECT COUNT(sg.businessIdentifier)" +
                                      " FROM smp_service_group sg, smp_ownership so" +
                                      " WHERE so.username=?" +
                                      " AND so.businessIdentifierScheme=sg.businessIdentifierScheme AND so.businessIdentifier=sg.businessIdentifier",
                                      new ConstantPreparedStatementDataProvider (sOwnerID));
  }

  @Nullable
  public SMPServiceGroup getSMPServiceGroupOfID (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("getSMPServiceGroupOfID(" +
                    (aParticipantID == null ? "null" : aParticipantID.getURIEncoded ()) +
                    ")");

    if (aParticipantID == null)
      return null;

    // Use cache
    SMPServiceGroup ret = m_aCache == null ? null : m_aCache.get (aParticipantID.getURIEncoded ());
    if (ret != null)
      return ret;

    // Not in cache
    final Wrapper <DBResultRow> aResult = new Wrapper <> ();
    newExecutor ().querySingle ("SELECT sg.extension, so.username" +
                                " FROM smp_service_group sg, smp_ownership so" +
                                " WHERE sg.businessIdentifierScheme=? AND sg.businessIdentifier=?" +
                                " AND so.businessIdentifierScheme=sg.businessIdentifierScheme AND so.businessIdentifier=sg.businessIdentifier",
                                new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                           aParticipantID.getValue ()),
                                aResult::set);
    if (aResult.isNotSet ())
      return null;

    ret = new SMPServiceGroup (aResult.get ().getAsString (1), aParticipantID, aResult.get ().getAsString (0));
    if (m_aCache != null)
      m_aCache.put (aParticipantID.getURIEncoded (), ret);
    return ret;
  }

  public boolean containsSMPServiceGroupWithID (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("containsSMPServiceGroupWithID(" +
                    (aParticipantID == null ? "null" : aParticipantID.getURIEncoded ()) +
                    ")");

    if (aParticipantID == null)
      return false;

    // Cache check first
    if (m_aCache != null && m_aCache.containsKey (aParticipantID.getURIEncoded ()))
      return true;

    return 1 == newExecutor ().queryCount ("SELECT COUNT(*) FROM smp_service_group" +
                                           " WHERE businessIdentifierScheme=? AND businessIdentifier=?",
                                           new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                                      aParticipantID.getValue ()));
  }

  @CheckForSigned
  public long getSMPServiceGroupCount ()
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("getSMPServiceGroupCount()");

    return newExecutor ().queryCount ("SELECT COUNT(*) FROM smp_service_group");
  }
}
