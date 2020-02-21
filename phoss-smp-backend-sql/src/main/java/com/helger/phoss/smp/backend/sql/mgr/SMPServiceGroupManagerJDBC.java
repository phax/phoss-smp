/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.backend.sql.mgr;

import java.util.Optional;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.mutable.MutableBoolean;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.db.jpa.JPAExecutionResult;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.backend.sql.AbstractJDBCEnabledManager;
import com.helger.phoss.smp.backend.sql.model.DBOwnership;
import com.helger.phoss.smp.backend.sql.model.DBOwnershipID;
import com.helger.phoss.smp.backend.sql.model.DBServiceGroup;
import com.helger.phoss.smp.backend.sql.model.DBServiceGroupID;
import com.helger.phoss.smp.backend.sql.model.DBUser;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupCallback;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.servicegroup.SMPServiceGroup;
import com.helger.phoss.smp.exception.SMPInternalErrorException;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPSMLException;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.exception.SMPUnknownUserException;
import com.helger.phoss.smp.smlhook.IRegistrationHook;
import com.helger.phoss.smp.smlhook.RegistrationHookException;
import com.helger.phoss.smp.smlhook.RegistrationHookFactory;

public final class SMPServiceGroupManagerJDBC extends AbstractJDBCEnabledManager implements ISMPServiceGroupManager
{
  private final CallbackList <ISMPServiceGroupCallback> m_aCBs = new CallbackList <> ();

  public SMPServiceGroupManagerJDBC ()
  {}

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <ISMPServiceGroupCallback> serviceGroupCallbacks ()
  {
    return m_aCBs;
  }

  @Nonnull
  public SMPServiceGroup createSMPServiceGroup (@Nonnull @Nonempty final String sOwnerID,
                                                @Nonnull final IParticipantIdentifier aParticipantID,
                                                @Nullable final String sExtension) throws SMPServerException
  {
    ValueEnforcer.notEmpty (sOwnerID, "OwnerID");
    ValueEnforcer.notNull (aParticipantID, "ParticpantID");
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createSMPServiceGroup (" +
                    sOwnerID +
                    ", " +
                    aParticipantID.getURIEncoded () +
                    ", " +
                    (StringHelper.hasText (sExtension) ? "with extension" : "without extension") +
                    ")");

    final IRegistrationHook aHook = RegistrationHookFactory.getInstance ();
    final MutableBoolean aCreatedServiceGroup = new MutableBoolean (false);

    executor ().performInTransaction ( () -> {

    });

    JPAExecutionResult <?> ret;
    ret = doInTransaction ( () -> {
      final EntityManager aEM = getEntityManager ();

      // Check if the passed service group ID is already in use
      final DBServiceGroupID aDBServiceGroupID = new DBServiceGroupID (aParticipantID);
      DBServiceGroup aDBServiceGroup = aEM.find (DBServiceGroup.class, aDBServiceGroupID);
      if (aDBServiceGroup != null)
        throw new IllegalStateException ("The service group with ID " +
                                         aParticipantID.getURIEncoded () +
                                         " already exists!");

      final DBUser aDBUser = aEM.find (DBUser.class, sOwnerID);
      if (aDBUser == null)
        throw new SMPUnknownUserException (sOwnerID);

      {
        // It's a new service group - Create in SML and remember that
        // Throws exception in case of an error
        aHook.createServiceGroup (aParticipantID);
        aCreatedServiceGroup.set (true);
      }

      // Did not exist. Create it.
      final DBOwnershipID aDBOwnershipID = new DBOwnershipID (sOwnerID, aParticipantID);
      final DBOwnership aOwnership = new DBOwnership (aDBOwnershipID, aDBUser);
      aDBServiceGroup = new DBServiceGroup (aDBServiceGroupID, sExtension, aOwnership, null);
      aEM.persist (aDBServiceGroup);
      aEM.persist (aOwnership);
    });

    if (ret.isFailure ())
    {
      // Error writing to the DB
      if (aCreatedServiceGroup.booleanValue ())
      {
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
    }

    if (ret.hasException ())
    {
      // Propagate contained exception
      final Exception ex = ret.getException ();
      if (ex instanceof SMPServerException)
        throw (SMPServerException) ex;
      if (ex instanceof RegistrationHookException)
        throw new SMPSMLException ("Failed to create '" + aParticipantID.getURIEncoded () + "' in SML",
                                   (RegistrationHookException) ex);
      throw new SMPInternalErrorException ("Error creating ServiceGroup '" + aParticipantID.getURIEncoded () + "'",
                                           ret.getException ());
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createSMPServiceGroup succeeded");

    final SMPServiceGroup aServiceGroup = new SMPServiceGroup (sOwnerID, aParticipantID, sExtension);
    m_aCBs.forEach (x -> x.onSMPServiceGroupCreated (aServiceGroup));
    return aServiceGroup;
  }

  @Nonnull
  public EChange updateSMPServiceGroup (@Nonnull final IParticipantIdentifier aParticipantID,
                                        @Nonnull @Nonempty final String sNewOwnerID,
                                        @Nullable final String sExtension) throws SMPServerException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notEmpty (sNewOwnerID, "NewOwnerID");
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("updateSMPServiceGroup (" +
                    aParticipantID.getURIEncoded () +
                    ", " +
                    sNewOwnerID +
                    ", " +
                    (StringHelper.hasText (sExtension) ? "with extension" : "without extension") +
                    ")");

    JPAExecutionResult <EChange> ret;
    ret = doInTransaction ( () -> {
      // Check if the passed service group ID is already in use
      final EntityManager aEM = getEntityManager ();
      final DBServiceGroup aDBServiceGroup = aEM.find (DBServiceGroup.class, new DBServiceGroupID (aParticipantID));
      if (aDBServiceGroup == null)
        return EChange.UNCHANGED;

      EChange eChange = EChange.UNCHANGED;
      final DBOwnership aOldOwnership = aDBServiceGroup.getOwnership ();
      if (!aOldOwnership.getId ().getUsername ().equals (sNewOwnerID))
      {
        // Update ownership

        // Is new owner existing?
        final DBUser aNewUser = aEM.find (DBUser.class, sNewOwnerID);
        if (aNewUser == null)
          throw new IllegalStateException ("User '" + sNewOwnerID + "' does not exist!");

        aEM.remove (aOldOwnership);

        // The business did exist. So it must be owned by the passed user.
        final DBOwnershipID aDBOwnershipID = new DBOwnershipID (sNewOwnerID, aParticipantID);
        aDBServiceGroup.setOwnership (new DBOwnership (aDBOwnershipID, aNewUser));

        eChange = EChange.CHANGED;
      }

      // Simply update the extension
      if (!EqualsHelper.equals (aDBServiceGroup.getExtension (), sExtension))
        eChange = EChange.CHANGED;
      aDBServiceGroup.setExtension (sExtension);

      aEM.merge (aDBServiceGroup);
      return eChange;
    });

    if (ret.hasException ())
    {
      final Exception ex = ret.getException ();
      if (ex instanceof SMPServerException)
        throw (SMPServerException) ex;
      throw new SMPInternalErrorException ("Failed to update ServiceGroup '" + aParticipantID.getURIEncoded () + "'",
                                           ret.getException ());
    }

    final EChange eChange = ret.get ();
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("updateSMPServiceGroup succeeded. Change=" + eChange.isChanged ());

    // Callback only if something changed
    if (eChange.isChanged ())
      m_aCBs.forEach (x -> x.onSMPServiceGroupUpdated (aParticipantID));

    return eChange;
  }

  @Nonnull
  public EChange deleteSMPServiceGroup (@Nonnull final IParticipantIdentifier aParticipantID) throws SMPServerException
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceGroup (" + aParticipantID.getURIEncoded () + ")");

    final IRegistrationHook aHook = RegistrationHookFactory.getInstance ();
    final MutableBoolean aDeletedServiceGroupInSML = new MutableBoolean (false);

    JPAExecutionResult <EChange> ret;
    ret = doInTransaction ( () -> {
      // Check if the service group is existing
      final EntityManager aEM = getEntityManager ();
      final DBServiceGroupID aDBServiceGroupID = new DBServiceGroupID (aParticipantID);
      final DBServiceGroup aDBServiceGroup = aEM.find (DBServiceGroup.class, aDBServiceGroupID);
      if (aDBServiceGroup == null)
        throw new SMPNotFoundException ("No such service group '" + aParticipantID.getURIEncoded () + "'");

      {
        // Delete in SML - and remember that
        // throws exception in case of error
        aHook.deleteServiceGroup (aParticipantID);
        aDeletedServiceGroupInSML.set (true);
      }

      aEM.remove (aDBServiceGroup);
      return EChange.CHANGED;
    });

    if (ret.isFailure ())
    {
      // Error writing to the DB
      if (aDeletedServiceGroupInSML.booleanValue ())
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

    if (ret.hasException ())
    {
      final Exception ex = ret.getException ();
      if (ex instanceof SMPServerException)
        throw (SMPServerException) ex;
      if (ex instanceof RegistrationHookException)
        throw new SMPSMLException ("Failed to delete '" + aParticipantID.getURIEncoded () + "' in SML",
                                   (RegistrationHookException) ex);
      throw new SMPInternalErrorException ("Failed to delete ServiceGroup '" + aParticipantID.getURIEncoded () + "'",
                                           ret.getException ());
    }

    final EChange eChange = ret.get ();
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceGroup succeeded. Change=" + eChange.isChanged ());

    if (eChange.isChanged ())
    {
      m_aCBs.forEach (x -> x.onSMPServiceGroupDeleted (aParticipantID));
    }

    return eChange;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroups ()
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("getAllSMPServiceGroups()");

    final Optional <ICommonsList <DBResultRow>> aDBResult = executor ().queryAll ("SELECT sg.businessIdentifierScheme, sg.businessIdentifier, sg.extension, so.username" +
                                                                                  " FROM smp_service_group AS sg, smp_ownership AS so" +
                                                                                  " AND so.businessIdentifierScheme=sg.businessIdentifierScheme AND so.businessIdentifier=sg.businessIdentifier");

    final ICommonsList <ISMPServiceGroup> ret = new CommonsArrayList <> ();
    if (aDBResult.isPresent ())
      for (final DBResultRow aRow : aDBResult.get ())
        ret.add (new SMPServiceGroup (aRow.getAsString (3),
                                      new SimpleParticipantIdentifier (aRow.getAsString (0), aRow.getAsString (1)),
                                      aRow.getAsString (2)));
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroupsOfOwner (@Nonnull final String sOwnerID)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("getAllSMPServiceGroupsOfOwner(" + sOwnerID + ")");

    final Optional <ICommonsList <DBResultRow>> aDBResult = executor ().queryAll ("SELECT sg.businessIdentifierScheme, sg.businessIdentifier, sg.extension" +
                                                                                  " FROM smp_service_group AS sg, smp_ownership AS so" +
                                                                                  " WHERE so.username=?" +
                                                                                  " AND so.businessIdentifierScheme=sg.businessIdentifierScheme AND so.businessIdentifier=sg.businessIdentifier",
                                                                                  new ConstantPreparedStatementDataProvider (sOwnerID));

    final ICommonsList <ISMPServiceGroup> ret = new CommonsArrayList <> ();
    if (aDBResult.isPresent ())
      for (final DBResultRow aRow : aDBResult.get ())
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

    return executor ().queryCount ("SELECT COUNT(sg.businessIdentifier)" +
                                   " FROM smp_service_group AS sg, smp_ownership AS so" +
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

    final Optional <DBResultRow> aResult = executor ().querySingle ("SELECT sg.extension, so.username" +
                                                                    " FROM smp_service_group AS sg, smp_ownership AS so" +
                                                                    " WHERE sg.businessIdentifierScheme=? AND sg.businessIdentifier=?" +
                                                                    " AND so.businessIdentifierScheme=sg.businessIdentifierScheme AND so.businessIdentifier=sg.businessIdentifier",
                                                                    new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                                                               aParticipantID.getValue ()));
    if (!aResult.isPresent ())
      return null;

    return new SMPServiceGroup (aResult.get ().getAsString (1), aParticipantID, aResult.get ().getAsString (0));
  }

  public boolean containsSMPServiceGroupWithID (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("containsSMPServiceGroupWithID(" +
                    (aParticipantID == null ? "null" : aParticipantID.getURIEncoded ()) +
                    ")");

    if (aParticipantID == null)
      return false;

    return 1 == executor ().queryCount ("SELECT COUNT(*) FROM smp_service_group WHERE businessIdentifierScheme=? AND businessIdentifier=?",
                                        new ConstantPreparedStatementDataProvider (aParticipantID.getScheme (),
                                                                                   aParticipantID.getValue ()));
  }

  @Nonnegative
  public long getSMPServiceGroupCount ()
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("getSMPServiceGroupCount()");

    return executor ().queryCount ("SELECT COUNT(*) FROM smp_service_group");
  }
}
