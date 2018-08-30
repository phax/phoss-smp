/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.data.sql.mgr;

import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import org.eclipse.persistence.config.CacheUsage;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.mutable.MutableBoolean;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.db.jpa.JPAExecutionResult;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.data.sql.AbstractSMPJPAEnabledManager;
import com.helger.peppol.smpserver.data.sql.model.DBOwnership;
import com.helger.peppol.smpserver.data.sql.model.DBOwnershipID;
import com.helger.peppol.smpserver.data.sql.model.DBServiceGroup;
import com.helger.peppol.smpserver.data.sql.model.DBServiceGroupID;
import com.helger.peppol.smpserver.data.sql.model.DBUser;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupCallback;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroup;
import com.helger.peppol.smpserver.smlhook.IRegistrationHook;
import com.helger.peppol.smpserver.smlhook.RegistrationHookFactory;

public final class SQLServiceGroupManager extends AbstractSMPJPAEnabledManager implements ISMPServiceGroupManager
{
  private final CallbackList <ISMPServiceGroupCallback> m_aCBs = new CallbackList <> ();

  public SQLServiceGroupManager ()
  {}

  @Nonnull
  @ReturnsMutableObject ("by design")
  public CallbackList <ISMPServiceGroupCallback> serviceGroupCallbacks ()
  {
    return m_aCBs;
  }

  @Nullable
  public SMPServiceGroup createSMPServiceGroup (@Nonnull @Nonempty final String sOwnerID,
                                                @Nonnull final IParticipantIdentifier aParticipantIdentifier,
                                                @Nullable final String sExtension)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createSMPServiceGroup (" +
                    sOwnerID +
                    ", " +
                    aParticipantIdentifier.getURIEncoded () +
                    ", " +
                    (StringHelper.hasText (sExtension) ? "with extension" : "without extension") +
                    ")");

    final IRegistrationHook aHook = RegistrationHookFactory.getInstance ();
    final MutableBoolean aCreatedServiceGroup = new MutableBoolean (false);

    JPAExecutionResult <?> ret;
    ret = doInTransaction ( () -> {
      final EntityManager aEM = getEntityManager ();

      // Check if the passed service group ID is already in use
      final DBServiceGroupID aDBServiceGroupID = new DBServiceGroupID (aParticipantIdentifier);
      DBServiceGroup aDBServiceGroup = aEM.find (DBServiceGroup.class, aDBServiceGroupID);
      if (aDBServiceGroup != null)
        throw new IllegalStateException ("The service group with ID " +
                                         aParticipantIdentifier.getURIEncoded () +
                                         " already exists!");

      final DBUser aDBUser = aEM.find (DBUser.class, sOwnerID);
      if (aDBUser == null)
        throw new IllegalStateException ("User '" + sOwnerID + "' does not exist!");

      {
        // It's a new service group - Create in SML and remember that
        // Throws exception in case of an error
        aHook.createServiceGroup (aParticipantIdentifier);
        aCreatedServiceGroup.set (true);
      }

      // Did not exist. Create it.
      final DBOwnershipID aDBOwnershipID = new DBOwnershipID (sOwnerID, aParticipantIdentifier);
      final DBOwnership aOwnership = new DBOwnership (aDBOwnershipID, aDBUser, aDBServiceGroup);
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
        aHook.undoCreateServiceGroup (aParticipantIdentifier);
      }
    }

    if (ret.hasException ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return null;
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("createSMPServiceGroup succeeded");

    final SMPServiceGroup aServiceGroup = new SMPServiceGroup (sOwnerID, aParticipantIdentifier, sExtension);
    m_aCBs.forEach (x -> x.onSMPServiceGroupCreated (aServiceGroup));
    return aServiceGroup;
  }

  @Nonnull
  public EChange updateSMPServiceGroup (@Nullable final String sSMPServiceGroupID,
                                        @Nonnull @Nonempty final String sNewOwnerID,
                                        @Nullable final String sExtension)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("updateSMPServiceGroup (" +
                    sSMPServiceGroupID +
                    ", " +
                    sNewOwnerID +
                    ", " +
                    (StringHelper.hasText (sExtension) ? "with extension" : "without extension") +
                    ")");

    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aParticipantIdentifier = aIdentifierFactory.parseParticipantIdentifier (sSMPServiceGroupID);
    if (aParticipantIdentifier == null)
      throw new IllegalStateException ("Failed to parse participant identifier '" + sSMPServiceGroupID + "'");

    JPAExecutionResult <EChange> ret;
    ret = doInTransaction ( () -> {
      // Check if the passed service group ID is already in use
      final EntityManager aEM = getEntityManager ();
      final DBServiceGroup aDBServiceGroup = aEM.find (DBServiceGroup.class,
                                                       new DBServiceGroupID (aParticipantIdentifier));
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
        final DBOwnershipID aDBOwnershipID = new DBOwnershipID (sNewOwnerID, aParticipantIdentifier);
        aDBServiceGroup.setOwnership (new DBOwnership (aDBOwnershipID, aNewUser, aDBServiceGroup));

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
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return EChange.UNCHANGED;
    }

    final EChange eChange = ret.get ();
    LOGGER.info ("updateSMPServiceGroup succeeded. Change=" + eChange.isChanged ());

    if (eChange.isChanged ())
      m_aCBs.forEach (x -> x.onSMPServiceGroupUpdated (sSMPServiceGroupID));

    return eChange;
  }

  @Nonnull
  public EChange deleteSMPServiceGroup (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (aParticipantID == null)
      return EChange.UNCHANGED;

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceGroup (" + aParticipantID.getURIEncoded () + ")");

    final IRegistrationHook aHook = RegistrationHookFactory.getInstance ();
    final MutableBoolean aDeletedServiceGroup = new MutableBoolean (false);

    JPAExecutionResult <EChange> ret;
    ret = doInTransaction ( () -> {
      // Check if the service group is existing
      final EntityManager aEM = getEntityManager ();
      final DBServiceGroupID aDBServiceGroupID = new DBServiceGroupID (aParticipantID);
      final DBServiceGroup aDBServiceGroup = aEM.find (DBServiceGroup.class, aDBServiceGroupID);
      if (aDBServiceGroup == null)
      {
        LOGGER.warn ("No such service group to delete: " + aParticipantID.getURIEncoded ());
        return EChange.UNCHANGED;
      }

      {
        // Delete in SML - and remember that
        // throws exception in case of error
        aHook.deleteServiceGroup (aParticipantID);
        aDeletedServiceGroup.set (true);
      }

      aEM.remove (aDBServiceGroup);
      return EChange.CHANGED;
    });

    if (ret.isFailure ())
    {
      // Error writing to the DB
      if (aDeletedServiceGroup.booleanValue ())
      {
        // Undo deletion in SML!
        aHook.undoDeleteServiceGroup (aParticipantID);
      }
    }

    if (ret.hasException ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return EChange.UNCHANGED;
    }

    final EChange eChange = ret.get ();
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("deleteSMPServiceGroup succeeded. Change=" + eChange.isChanged ());

    if (eChange.isChanged ())
    {
      final String sServiceGroupID = SMPServiceGroup.createSMPServiceGroupID (aParticipantID);
      m_aCBs.forEach (x -> x.onSMPServiceGroupDeleted (sServiceGroupID));
    }

    return eChange;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroups ()
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("getAllSMPServiceGroups()");

    JPAExecutionResult <ICommonsList <ISMPServiceGroup>> ret;
    ret = doSelect ( () -> {
      final List <DBServiceGroup> aDBServiceGroups = getEntityManager ().createQuery ("SELECT p FROM DBServiceGroup p",
                                                                                      DBServiceGroup.class)
                                                                        .getResultList ();

      final ICommonsList <ISMPServiceGroup> aList = new CommonsArrayList <> ();
      for (final DBServiceGroup aDBServiceGroup : aDBServiceGroups)
      {
        final DBOwnership aDBOwnership = aDBServiceGroup.getOwnership ();
        if (aDBOwnership == null)
          throw new IllegalStateException ("Service group " +
                                           aDBServiceGroup.getId ().getAsBusinessIdentifier ().getURIEncoded () +
                                           " has no owner");

        final SMPServiceGroup aServiceGroup = new SMPServiceGroup (aDBOwnership.getId ().getUsername (),
                                                                   aDBServiceGroup.getId ().getAsBusinessIdentifier (),
                                                                   aDBServiceGroup.getExtension ());
        aList.add (aServiceGroup);
      }
      return aList;
    });
    if (ret.hasException ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return new CommonsArrayList <> ();
    }
    return ret.get ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPServiceGroup> getAllSMPServiceGroupsOfOwner (@Nonnull final String sOwnerID)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("getAllSMPServiceGroupsOfOwner(" + sOwnerID + ")");

    JPAExecutionResult <ICommonsList <ISMPServiceGroup>> ret;
    ret = doSelect ( () -> {
      final List <DBServiceGroup> aDBServiceGroups = getEntityManager ().createQuery ("SELECT p FROM DBServiceGroup p WHERE p.ownership.user.userName = :user",
                                                                                      DBServiceGroup.class)
                                                                        .setParameter ("user", sOwnerID)
                                                                        .getResultList ();

      final ICommonsList <ISMPServiceGroup> aList = new CommonsArrayList <> ();
      for (final DBServiceGroup aDBServiceGroup : aDBServiceGroups)
      {
        aList.add (new SMPServiceGroup (sOwnerID,
                                        aDBServiceGroup.getId ().getAsBusinessIdentifier (),
                                        aDBServiceGroup.getExtension ()));
      }
      return aList;
    });
    if (ret.hasException ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return new CommonsArrayList <> ();
    }
    return ret.get ();
  }

  @Nonnegative
  public int getSMPServiceGroupCountOfOwner (@Nonnull final String sOwnerID)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("getSMPServiceGroupCountOfOwner(" + sOwnerID + ")");

    JPAExecutionResult <Long> ret;
    ret = doSelect ( () -> {
      final long nCount = getSelectCountResult (getEntityManager ().createQuery ("SELECT COUNT(p) FROM DBOwnership p WHERE p.user.userName = :user",
                                                                                 DBOwnership.class)
                                                                   .setParameter ("user", sOwnerID));
      return Long.valueOf (nCount);
    });
    if (ret.hasException ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return 0;
    }
    return ret.get ().intValue ();
  }

  @Nullable
  public SMPServiceGroup getSMPServiceGroupOfID (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("getSMPServiceGroupOfID(" +
                    (aParticipantIdentifier == null ? "null" : aParticipantIdentifier.getURIEncoded ()) +
                    ")");

    if (aParticipantIdentifier == null)
      return null;

    JPAExecutionResult <SMPServiceGroup> ret;
    ret = doSelect ( () -> {
      final DBServiceGroup aDBServiceGroup = getEntityManager ().find (DBServiceGroup.class,
                                                                       new DBServiceGroupID (aParticipantIdentifier));
      if (aDBServiceGroup == null)
        return null;

      return new SMPServiceGroup (aDBServiceGroup.getOwnership ().getId ().getUsername (),
                                  aDBServiceGroup.getId ().getAsBusinessIdentifier (),
                                  aDBServiceGroup.getExtension ());
    });
    if (ret.hasException ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return null;
    }
    return ret.get ();
  }

  public boolean containsSMPServiceGroupWithID (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("containsSMPServiceGroupWithID(" +
                    (aParticipantIdentifier == null ? "null" : aParticipantIdentifier.getURIEncoded ()) +
                    ")");

    if (aParticipantIdentifier == null)
      return false;

    JPAExecutionResult <Boolean> ret;
    ret = doSelect ( () -> {
      // Disable caching here
      final ICommonsMap <String, Object> aProps = new CommonsHashMap <> ();
      aProps.put ("eclipselink.cache-usage", CacheUsage.DoNotCheckCache);
      final DBServiceGroup aDBServiceGroup = getEntityManager ().find (DBServiceGroup.class,
                                                                       new DBServiceGroupID (aParticipantIdentifier),
                                                                       aProps);
      return Boolean.valueOf (aDBServiceGroup != null);
    });
    if (ret.hasException ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return false;
    }
    return ret.get ().booleanValue ();
  }

  @Nonnegative
  public int getSMPServiceGroupCount ()
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("getSMPServiceGroupCount()");

    JPAExecutionResult <Long> ret;
    ret = doSelect ( () -> {
      final long nCount = getSelectCountResult (getEntityManager ().createQuery ("SELECT COUNT(p.id) FROM DBServiceGroup p"));
      return Long.valueOf (nCount);
    });
    if (ret.hasException ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return 0;
    }
    return ret.get ().intValue ();
  }
}
