/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver.data.sql.mgr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import org.eclipse.persistence.config.CacheUsage;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.equals.EqualsHelper;
import com.helger.commons.lang.ClassHelper;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
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
    setUseTransactionsForSelect (true);
  }

  @Nonnull
  public SMPServiceGroup createSMPServiceGroup (@Nonnull @Nonempty final String sOwnerID,
                                                @Nonnull final IParticipantIdentifier aParticipantIdentifier,
                                                @Nullable final String sExtension)
  {
    s_aLogger.info ("createSMPServiceGroup (" +
                    sOwnerID +
                    ", " +
                    IdentifierHelper.getIdentifierURIEncoded (aParticipantIdentifier) +
                    ", " +
                    (StringHelper.hasText (sExtension) ? "with extension" : "without extension") +
                    ")");

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
          final DBOwnership aOwnership = new DBOwnership (aDBOwnershipID, aDBUser, aDBServiceGroup);
          aDBServiceGroup = new DBServiceGroup (aDBServiceGroupID, sExtension, aOwnership, null);
          aEM.persist (aDBServiceGroup);
          aEM.persist (aOwnership);
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

    s_aLogger.info ("createSMPServiceGroup succeeded");
    return new SMPServiceGroup (sOwnerID, aParticipantIdentifier, sExtension);
  }

  @Nonnull
  public EChange updateSMPServiceGroup (@Nullable final String sSMPServiceGroupID,
                                        @Nonnull @Nonempty final String sNewOwnerID,
                                        @Nullable final String sExtension)
  {
    s_aLogger.info ("updateSMPServiceGroup (" +
                    sSMPServiceGroupID +
                    ", " +
                    sNewOwnerID +
                    ", " +
                    (StringHelper.hasText (sExtension) ? "with extension" : "without extension") +
                    ")");

    final IParticipantIdentifier aParticipantIdentifier = IdentifierHelper.createParticipantIdentifierFromURIPartOrNull (sSMPServiceGroupID);
    JPAExecutionResult <EChange> ret;
    ret = doInTransaction (new Callable <EChange> ()
    {
      @Nonnull
      public EChange call ()
      {
        // Check if the passed service group ID is already in use
        final EntityManager aEM = getEntityManager ();
        final DBServiceGroup aDBServiceGroup = aEM.find (DBServiceGroup.class, new DBServiceGroupID (aParticipantIdentifier));
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

    s_aLogger.info ("updateSMPServiceGroup succeeded");
    return ret.get ();
  }

  @Nonnull
  public EChange deleteSMPServiceGroup (@Nullable final IParticipantIdentifier aParticipantID)
  {
    s_aLogger.info ("deleteSMPServiceGroup (" + IdentifierHelper.getIdentifierURIEncoded (aParticipantID) + ")");

    JPAExecutionResult <EChange> ret;
    ret = doInTransaction (new Callable <EChange> ()
    {
      @Nonnull
      public EChange call ()
      {
        // Check if the service group is existing
        final EntityManager aEM = getEntityManager ();
        final DBServiceGroupID aDBServiceGroupID = new DBServiceGroupID (aParticipantID);
        final DBServiceGroup aDBServiceGroup = aEM.find (DBServiceGroup.class, aDBServiceGroupID);
        if (aDBServiceGroup == null)
        {
          s_aLogger.warn ("No such service group to delete: " + IdentifierHelper.getIdentifierURIEncoded (aParticipantID));
          return EChange.UNCHANGED;
        }

        // Delete in SML - throws exception in case of error
        m_aHook.deleteServiceGroup (aParticipantID);

        try
        {
          aEM.remove (aDBServiceGroup);
        }
        catch (final RuntimeException ex)
        {
          // An error occurred - remove from SML again
          m_aHook.undoDeleteServiceGroup (aParticipantID);
          throw ex;
        }

        return EChange.CHANGED;
      }
    });
    if (ret.hasThrowable ())
    {
      s_aLogger.info ("deleteSMPServiceGroup failed. Throwable: " + ClassHelper.getClassLocalName (ret.getThrowable ()));
      throw new RuntimeException (ret.getThrowable ());
    }

    s_aLogger.info ("deleteSMPServiceGroup succeeded. Change=" + ret.get ().isChanged ());
    return ret.get ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPServiceGroup> getAllSMPServiceGroups ()
  {
    s_aLogger.info ("getAllSMPServiceGroups()");

    JPAExecutionResult <Collection <ISMPServiceGroup>> ret;
    ret = doSelect (new Callable <Collection <ISMPServiceGroup>> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public Collection <ISMPServiceGroup> call () throws Exception
      {
        final List <DBServiceGroup> aDBServiceGroups = getEntityManager ().createQuery ("SELECT p FROM DBServiceGroup p", DBServiceGroup.class)
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
                                                                     aDBServiceGroup.getId ().getAsBusinessIdentifier (),
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
    s_aLogger.info ("getAllSMPServiceGroupsOfOwner(" + sOwnerID + ")");

    JPAExecutionResult <Collection <ISMPServiceGroup>> ret;
    ret = doSelect (new Callable <Collection <ISMPServiceGroup>> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public Collection <ISMPServiceGroup> call () throws Exception
      {
        final List <DBServiceGroup> aDBServiceGroups = getEntityManager ().createQuery ("SELECT p FROM DBServiceGroup p WHERE p.ownership.user.userName = :user",
                                                                                        DBServiceGroup.class)
                                                                          .setParameter ("user", sOwnerID)
                                                                          .getResultList ();

        final Collection <ISMPServiceGroup> aList = new ArrayList <> ();
        for (final DBServiceGroup aDBServiceGroup : aDBServiceGroups)
        {
          final SMPServiceGroup aServiceGroup = new SMPServiceGroup (sOwnerID,
                                                                     aDBServiceGroup.getId ().getAsBusinessIdentifier (),
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

  @Nonnegative
  public int getSMPServiceGroupCountOfOwner (@Nonnull final String sOwnerID)
  {
    s_aLogger.info ("getSMPServiceGroupCountOfOwner(" + sOwnerID + ")");

    JPAExecutionResult <Long> ret;
    ret = doSelect (new Callable <Long> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public Long call () throws Exception
      {
        final long nCount = getSelectCountResult (getEntityManager ().createQuery ("SELECT COUNT(p) FROM DBOwnership p WHERE p.user.userName = :user",
                                                                                   DBOwnership.class)
                                                                     .setParameter ("user", sOwnerID));
        return Long.valueOf (nCount);
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return ret.get ().intValue ();
  }

  @Nullable
  public ISMPServiceGroup getSMPServiceGroupOfID (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    s_aLogger.info ("getSMPServiceGroupOfID(" +
                    (aParticipantIdentifier == null ? "null" : IdentifierHelper.getIdentifierURIEncoded (aParticipantIdentifier)) +
                    ")");

    if (aParticipantIdentifier == null)
      return null;

    JPAExecutionResult <ISMPServiceGroup> ret;
    ret = doSelect (new Callable <ISMPServiceGroup> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public ISMPServiceGroup call () throws Exception
      {
        final DBServiceGroup aDBServiceGroup = getEntityManager ().find (DBServiceGroup.class, new DBServiceGroupID (aParticipantIdentifier));
        if (aDBServiceGroup == null)
          return null;

        final SMPServiceGroup aServiceGroup = new SMPServiceGroup (aDBServiceGroup.getOwnership ().getId ().getUsername (),
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
    s_aLogger.info ("containsSMPServiceGroupWithID(" +
                    (aParticipantIdentifier == null ? "null" : IdentifierHelper.getIdentifierURIEncoded (aParticipantIdentifier)) +
                    ")");

    if (aParticipantIdentifier == null)
      return false;

    JPAExecutionResult <Boolean> ret;
    ret = doSelect (new Callable <Boolean> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public Boolean call () throws Exception
      {
        // Disable caching here
        final Map <String, Object> aProps = new HashMap <> ();
        aProps.put ("eclipselink.cache-usage", CacheUsage.DoNotCheckCache);
        final DBServiceGroup aDBServiceGroup = getEntityManager ().find (DBServiceGroup.class, new DBServiceGroupID (aParticipantIdentifier), aProps);
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
    s_aLogger.info ("getSMPServiceGroupCount()");

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
