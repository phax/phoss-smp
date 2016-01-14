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

import java.util.Collection;
import java.util.concurrent.Callable;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.callback.IThrowingCallable;
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
import com.helger.peppol.smpserver.domain.user.ISMPUser;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.exception.SMPNotFoundException;
import com.helger.peppol.smpserver.exception.SMPServerException;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;
import com.helger.peppol.smpserver.exception.SMPUnknownUserException;
import com.helger.web.http.basicauth.BasicAuthClientCredentials;

/**
 * A EclipseLink based implementation of the {@link ISMPUserManager} interface.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class SQLUserManager extends AbstractSMPJPAEnabledManager implements ISMPUserManager
{
  public SQLUserManager ()
  {}

  public boolean isSpecialUserManagementNeeded ()
  {
    return true;
  }

  public void createUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    doInTransaction (new Runnable ()
    {
      public void run ()
      {
        final DBUser aDBUser = new DBUser (sUserName, sPassword);
        getEntityManager ().persist (aDBUser);
      }
    });
  }

  public void updateUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    doInTransaction (new Runnable ()
    {
      public void run ()
      {
        final DBUser aDBUser = getEntityManager ().find (DBUser.class, sUserName);
        if (aDBUser != null)
        {
          aDBUser.setPassword (sPassword);
          getEntityManager ().merge (aDBUser);
        }
      }
    });
  }

  public void deleteUser (@Nullable final String sUserName)
  {
    if (StringHelper.hasText (sUserName))
      doInTransaction (new Runnable ()
      {
        public void run ()
        {
          final EntityManager aEM = getEntityManager ();
          final DBUser aDBUser = aEM.find (DBUser.class, sUserName);
          if (aDBUser != null)
            aEM.remove (aDBUser);
        }
      });
  }

  @Nonnegative
  public int getUserCount ()
  {
    JPAExecutionResult <Long> ret;
    ret = doSelect (new Callable <Long> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public Long call () throws Exception
      {
        final long nCount = getSelectCountResult (getEntityManager ().createQuery ("SELECT COUNT(p) FROM DBUser p"));
        return Long.valueOf (nCount);
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return ret.get ().intValue ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <DBUser> getAllUsers ()
  {
    JPAExecutionResult <Collection <DBUser>> ret;
    ret = doSelect (new Callable <Collection <DBUser>> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public Collection <DBUser> call () throws Exception
      {
        return getEntityManager ().createQuery ("SELECT p FROM DBUser p", DBUser.class).getResultList ();
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return ret.get ();
  }

  @Nullable
  public DBUser getUserOfID (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return null;

    JPAExecutionResult <DBUser> ret;
    ret = doSelect (new Callable <DBUser> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public DBUser call () throws Exception
      {
        return getEntityManager ().find (DBUser.class, sID);
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return ret.get ();
  }

  @Nonnull
  public DBUser validateUserCredentials (@Nonnull final BasicAuthClientCredentials aCredentials) throws Throwable
  {
    JPAExecutionResult <DBUser> ret;
    ret = doSelect (new IThrowingCallable <DBUser, SMPServerException> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public DBUser call () throws SMPServerException
      {
        final String sUserName = aCredentials.getUserName ();
        final DBUser aDBUser = getEntityManager ().find (DBUser.class, sUserName);

        // Check that the user exists
        if (aDBUser == null)
          throw new SMPUnknownUserException (sUserName);

        // Check that the password is correct
        if (!aDBUser.getPassword ().equals (aCredentials.getPassword ()))
          throw new SMPUnauthorizedException ("Illegal password for user '" + sUserName + "'");

        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Verified credentials of user '" + sUserName + "' successfully");
        return aDBUser;
      }
    });
    return ret.getOrThrow ();
  }

  @Nonnull
  public DBOwnership verifyOwnership (@Nonnull final IParticipantIdentifier aServiceGroupID,
                                      @Nonnull final ISMPUser aCredentials) throws SMPUnauthorizedException
  {
    // Resolve service group
    // to throw a 404 if a service group does not exist
    final DBServiceGroup aServiceGroup = getEntityManager ().find (DBServiceGroup.class, new DBServiceGroupID (aServiceGroupID));
    if (aServiceGroup == null)
    {
      throw new SMPNotFoundException ("Service group " + IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID) + " does not exist");
    }

    final DBOwnershipID aOwnershipID = new DBOwnershipID (aCredentials.getID (), aServiceGroupID);
    final DBOwnership aOwnership = getEntityManager ().find (DBOwnership.class, aOwnershipID);
    if (aOwnership == null)
    {
      throw new SMPUnauthorizedException ("User '" +
                                          aCredentials.getUserName () +
                                          "' does not own " +
                                          IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID));
    }

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Verified service group ID " +
                       IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID) +
                       " is owned by user '" +
                       aCredentials.getUserName () +
                       "'");
    return aOwnership;
  }
}
