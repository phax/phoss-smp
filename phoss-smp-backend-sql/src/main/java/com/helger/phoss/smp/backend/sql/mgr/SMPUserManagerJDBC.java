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

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.wrapper.Wrapper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.backend.sql.AbstractJDBCEnabledManager;
import com.helger.phoss.smp.backend.sql.model.DBUser;
import com.helger.phoss.smp.domain.user.ISMPUser;
import com.helger.phoss.smp.domain.user.ISMPUserManager;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.exception.SMPUnauthorizedException;
import com.helger.phoss.smp.exception.SMPUnknownUserException;

/**
 * A JDBC based implementation of the {@link ISMPUserManager} interface.
 *
 * @author Philip Helger
 * @since 5.3.0
 */
public final class SMPUserManagerJDBC extends AbstractJDBCEnabledManager implements ISMPUserManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPUserManagerJDBC.class);

  public SMPUserManagerJDBC ()
  {}

  public boolean isSpecialUserManagementNeeded ()
  {
    return true;
  }

  @Nonnull
  public ESuccess createUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    final Wrapper <ESuccess> ret = new Wrapper <> (ESuccess.FAILURE);
    executor ().performInTransaction ( () -> {
      if (getUserOfID (sUserName) != null)
        ret.set (ESuccess.FAILURE);
      else
      {
        final long nCount = executor ().insertOrUpdateOrDelete ("INSERT INTO smp_user (username, password) VALUES (?,?)",
                                                                new ConstantPreparedStatementDataProvider (sUserName, sPassword));
        ret.set (ESuccess.valueOf (nCount == 1));
      }
    });
    return ret.get ();
  }

  @Nonnull
  public ESuccess updateUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    final long nCount = executor ().insertOrUpdateOrDelete ("UPDATE smp_user SET password=? WHERE username=?",
                                                            new ConstantPreparedStatementDataProvider (sPassword, sUserName));
    return ESuccess.valueOf (nCount == 1);
  }

  @Nonnull
  public EChange deleteUser (@Nullable final String sUserName)
  {
    if (StringHelper.hasNoText (sUserName))
      return EChange.UNCHANGED;

    final long nCount = executor ().insertOrUpdateOrDelete ("DELETE FROM smp_user WHERE username=?",
                                                            new ConstantPreparedStatementDataProvider (sUserName));
    return EChange.valueOf (nCount == 1);
  }

  @Nonnegative
  public long getUserCount ()
  {
    return executor ().queryCount ("SELECT COUNT(*) FROM smp_user");
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPUser> getAllUsers ()
  {
    final Optional <ICommonsList <DBResultRow>> aDBResult = executor ().queryAll ("SELECT (username, password) FROM smp_user");
    final ICommonsList <ISMPUser> ret = new CommonsArrayList <> ();
    if (aDBResult.isPresent ())
      for (final DBResultRow aRow : aDBResult.get ())
        ret.add (new DBUser (aRow.getAsString (0), aRow.getAsString (1)));
    return ret;
  }

  @Nullable
  public DBUser getUserOfID (@Nullable final String sUserName)
  {
    if (StringHelper.hasNoText (sUserName))
      return null;

    final Optional <DBResultRow> aDBResult = executor ().querySingle ("SELECT password FROM smp_user WHERE username=?",
                                                                      new ConstantPreparedStatementDataProvider (sUserName));
    if (!aDBResult.isPresent ())
      return null;
    return new DBUser (sUserName, aDBResult.get ().getAsString (0));
  }

  @Nonnull
  public DBUser validateUserCredentials (@Nonnull final BasicAuthClientCredentials aCredentials) throws SMPServerException
  {
    final String sUserName = aCredentials.getUserName ();
    final DBUser aDBUser = getUserOfID (sUserName);

    // Check that the user exists
    if (aDBUser == null)
      throw new SMPUnknownUserException (sUserName);

    // Check that the password is correct
    if (!aDBUser.getPassword ().equals (aCredentials.getPassword ()))
      throw new SMPUnauthorizedException ("Illegal password for user '" + sUserName + "'");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Verified credentials of user '" + sUserName + "' successfully");
    return aDBUser;
  }

  public void verifyOwnership (@Nonnull final IParticipantIdentifier aServiceGroupID,
                               @Nonnull final ISMPUser aCredentials) throws SMPServerException
  {
    final long nCount = executor ().queryCount ("SELECT COUNT(*) FROM smp_ownership WHERE businessIdentifierScheme=? AND businessIdentifier=? AND username=?",
                                                new ConstantPreparedStatementDataProvider (aServiceGroupID.getScheme (),
                                                                                           aServiceGroupID.getValue (),
                                                                                           aCredentials.getUserName ()));
    if (nCount == 0)
    {
      throw new SMPUnauthorizedException ("User '" + aCredentials.getUserName () + "' does not own " + aServiceGroupID.getURIEncoded ());
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Verified service group ID " +
                    aServiceGroupID.getURIEncoded () +
                    " is owned by user '" +
                    aCredentials.getUserName () +
                    "'");
  }

  public void updateOwnerships (@Nonnull final ICommonsMap <String, String> aOldToNewMap)
  {
    if (aOldToNewMap.isNotEmpty ())
    {
      executor ().performInTransaction ( () -> {
        for (final Map.Entry <String, String> aEntry : aOldToNewMap.entrySet ())
        {
          final String sOld = aEntry.getKey ();
          final String sNew = aEntry.getValue ();
          executor ().insertOrUpdateOrDelete ("UPDATE smp_ownership SET username=? WHERE username=?",
                                              new ConstantPreparedStatementDataProvider (sNew, sOld));
        }
      });
    }
  }
}
