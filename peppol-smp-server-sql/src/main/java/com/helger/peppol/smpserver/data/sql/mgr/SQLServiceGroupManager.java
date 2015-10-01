/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.data.sql.mgr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.state.EChange;
import com.helger.db.jpa.JPAExecutionResult;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.smpserver.data.sql.AbstractSMPJPAEnabledManager;
import com.helger.peppol.smpserver.data.sql.model.DBOwnership;
import com.helger.peppol.smpserver.data.sql.model.DBOwnershipID;
import com.helger.peppol.smpserver.data.sql.model.DBServiceGroup;
import com.helger.peppol.smpserver.data.sql.model.DBServiceGroupID;
import com.helger.peppol.smpserver.data.sql.model.DBUser;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroup;
import com.helger.peppol.smpserver.smlhook.IRegistrationHook;
import com.helger.peppol.smpserver.smlhook.RegistrationHookFactory;

public final class SQLServiceGroupManager extends AbstractSMPJPAEnabledManager implements ISMPServiceGroupManager
{
  private final IRegistrationHook m_aHook;

  public SQLServiceGroupManager ()
  {
    m_aHook = RegistrationHookFactory.getOrCreateInstance ();
  }

  @Nonnull
  public SMPServiceGroup createSMPServiceGroup (@Nonnull @Nonempty final String sOwnerID,
                                                @Nonnull final IParticipantIdentifier aParticipantIdentifier,
                                                @Nullable final String sExtension)
  {
    JPAExecutionResult <?> ret;
    ret = doInTransaction (new Runnable ()
    {
      public void run ()
      {
        final EntityManager aEM = getEntityManager ();

        // Check if the passed service group ID is already in use
        final DBServiceGroupID aDBServiceGroupID = new DBServiceGroupID (aParticipantIdentifier);
        DBServiceGroup aDBServiceGroup = aEM.find (DBServiceGroup.class, aDBServiceGroupID);
        if (aDBServiceGroup != null)
          throw new IllegalStateException ("The service group with ID " +
                                           IdentifierHelper.getIdentifierURIEncoded (aParticipantIdentifier) +
                                           " already exists!");

        final DBUser aDBUser = aEM.find (DBUser.class, sOwnerID);
        if (aDBUser == null)
          throw new IllegalStateException ("User '" + sOwnerID + "' does not exist!");

        // It's a new service group - throws exception in case of an error
        m_aHook.createServiceGroup (aParticipantIdentifier);

        try
        {
          // Did not exist. Create it.
          final DBOwnershipID aDBOwnershipID = new DBOwnershipID (sOwnerID, aParticipantIdentifier);
          aDBServiceGroup = new DBServiceGroup (aDBServiceGroupID,
                                                sExtension,
                                                new DBOwnership (aDBOwnershipID, aDBUser, aDBServiceGroup),
                                                null);
          aEM.persist (aDBServiceGroup);
        }
        catch (final RuntimeException ex)
        {
          // An error occurred - remove from SML again
          m_aHook.undoCreateServiceGroup (aParticipantIdentifier);
          throw ex;
        }
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return new SMPServiceGroup (sOwnerID, aParticipantIdentifier, sExtension);
  }

  @Nonnull
  public EChange updateSMPServiceGroup (@Nullable final String sSMPServiceGroupID,
                                        @Nonnull @Nonempty final String sNewOwnerID,
                                        @Nullable final String sExtension)
  {
    final IParticipantIdentifier aParticipantIdentifier = IdentifierHelper.createParticipantIdentifierFromURIPartOrNull (sSMPServiceGroupID);
    JPAExecutionResult <EChange> ret;
    ret = doInTransaction (new Callable <EChange> ()
    {
      @Nonnull
      public EChange call ()
      {
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
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return ret.get ();
  }

  @Nonnull
  public EChange deleteSMPServiceGroup (@Nullable final IParticipantIdentifier aServiceGroupID)
  {
    JPAExecutionResult <EChange> ret;
    ret = doInTransaction (new Callable <EChange> ()
    {
      @Nonnull
      public EChange call ()
      {
        // Check if the service group is existing
        final EntityManager aEM = getEntityManager ();
        final DBServiceGroupID aDBServiceGroupID = new DBServiceGroupID (aServiceGroupID);
        final DBServiceGroup aDBServiceGroup = aEM.find (DBServiceGroup.class, aDBServiceGroupID);
        if (aDBServiceGroup == null)
        {
          s_aLogger.warn ("No such service group to delete: " +
                          IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID));
          return EChange.UNCHANGED;
        }

        // Check the ownership afterwards, so that only existing serviceGroups
        // are checked
        aEM.createQuery ("DELETE from DBOwnership p WHERE p.id.businessIdentifierScheme = :scheme AND p.id.businessIdentifier = :value")
           .setParameter ("scheme", aServiceGroupID.getScheme ())
           .setParameter ("value", aServiceGroupID.getValue ())
           .executeUpdate ();

        aEM.remove (aDBServiceGroup);
        return EChange.CHANGED;
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return ret.get ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPServiceGroup> getAllSMPServiceGroups ()
  {
    JPAExecutionResult <Collection <ISMPServiceGroup>> ret;
    ret = doSelect (new Callable <Collection <ISMPServiceGroup>> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public Collection <ISMPServiceGroup> call () throws Exception
      {
        final List <DBServiceGroup> aDBServiceGroups = getEntityManager ().createQuery ("SELECT p FROM DBServiceGroup p",
                                                                                        DBServiceGroup.class)
                                                                          .getResultList ();

        final Collection <ISMPServiceGroup> aList = new ArrayList <> ();
        for (final DBServiceGroup aDBServiceGroup : aDBServiceGroups)
        {
          final DBOwnership aDBOwnership = aDBServiceGroup.getOwnership ();
          if (aDBOwnership == null)
            throw new IllegalStateException ("Service group " +
                                             aDBServiceGroup.getId ().getAsBusinessIdentifier ().getURIEncoded () +
                                             " has no owner");

          final SMPServiceGroup aServiceGroup = new SMPServiceGroup (aDBOwnership.getId ().getUsername (),
                                                                     aDBServiceGroup.getId ()
                                                                                    .getAsBusinessIdentifier (),
                                                                     aDBServiceGroup.getExtension ());
          aList.add (aServiceGroup);
        }
        return aList;
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return ret.get ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPServiceGroup> getAllSMPServiceGroupsOfOwner (@Nonnull final String sOwnerID)
  {
    JPAExecutionResult <Collection <ISMPServiceGroup>> ret;
    ret = doSelect (new Callable <Collection <ISMPServiceGroup>> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public Collection <ISMPServiceGroup> call () throws Exception
      {
        final List <DBOwnership> aDBOwnerships = getEntityManager ().createQuery ("SELECT p FROM DBOwnership p WHERE p.user.userName = :user",
                                                                                  DBOwnership.class)
                                                                    .setParameter ("user", sOwnerID)
                                                                    .getResultList ();

        final Collection <ISMPServiceGroup> aList = new ArrayList <> ();
        for (final DBOwnership aDBOwnership : aDBOwnerships)
        {
          final SMPServiceGroup aServiceGroup = new SMPServiceGroup (sOwnerID,
                                                                     aDBOwnership.getServiceGroup ()
                                                                                 .getId ()
                                                                                 .getAsBusinessIdentifier (),
                                                                     aDBOwnership.getServiceGroup ().getExtension ());
          aList.add (aServiceGroup);
        }
        return aList;
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return ret.get ();
  }

  @Nullable
  public ISMPServiceGroup getSMPServiceGroupOfID (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    if (aParticipantIdentifier == null)
      return null;

    JPAExecutionResult <ISMPServiceGroup> ret;
    ret = doSelect (new Callable <ISMPServiceGroup> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public ISMPServiceGroup call () throws Exception
      {
        final DBServiceGroup aDBServiceGroup = getEntityManager ().find (DBServiceGroup.class,
                                                                         new DBServiceGroupID (aParticipantIdentifier));
        if (aDBServiceGroup == null)
          return null;

        final SMPServiceGroup aServiceGroup = new SMPServiceGroup (aDBServiceGroup.getOwnership ()
                                                                                  .getId ()
                                                                                  .getUsername (),
                                                                   aDBServiceGroup.getId ().getAsBusinessIdentifier (),
                                                                   aDBServiceGroup.getExtension ());
        return aServiceGroup;
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return ret.get ();
  }

  public boolean containsSMPServiceGroupWithID (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    if (aParticipantIdentifier == null)
      return false;

    JPAExecutionResult <Boolean> ret;
    ret = doSelect (new Callable <Boolean> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public Boolean call () throws Exception
      {
        final DBServiceGroup aDBServiceGroup = getEntityManager ().find (DBServiceGroup.class,
                                                                         new DBServiceGroupID (aParticipantIdentifier));
        return Boolean.valueOf (aDBServiceGroup != null);
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return ret.get ().booleanValue ();
  }

  @Nonnegative
  public int getSMPServiceGroupCount ()
  {
    JPAExecutionResult <Long> ret;
    ret = doSelect (new Callable <Long> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public Long call () throws Exception
      {
        final long nCount = getSelectCountResult (getEntityManager ().createQuery ("SELECT COUNT(p.id) FROM DBServiceGroup p"));
        return Long.valueOf (nCount);
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return ret.get ().intValue ();
  }
}
