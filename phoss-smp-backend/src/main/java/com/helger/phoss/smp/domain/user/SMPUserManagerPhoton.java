/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.user;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPUnauthorizedException;
import com.helger.phoss.smp.exception.SMPUnknownUserException;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.token.user.IUserToken;
import com.helger.photon.security.token.user.IUserTokenManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserManager;

/**
 * User management sanity methods
 *
 * @author Philip Helger
 */
public final class SMPUserManagerPhoton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPUserManagerPhoton.class);

  private SMPUserManagerPhoton ()
  {}

  /**
   * Check if the provided credentials are valid. This checks if the user
   * exists, if it is not deleted, if the password matches and if the user is
   * not disabled. If valid, the resolved user is returned.
   *
   * @param aCredentials
   *        The credentials to check. May not be <code>null</code>.
   * @return <code>null</code> if something does wrong, the user on success
   *         only.
   * @throws SMPUnknownUserException
   *         if the user does not exist or if the user is marked as deleted.
   * @throws SMPUnauthorizedException
   *         If the password is invalid or if the user is marked as disabled
   */
  @NonNull
  public static IUser validateUserCredentials (@NonNull final SMPAPICredentials aCredentials) throws SMPUnknownUserException,
                                                                                              SMPUnauthorizedException
  {
    ValueEnforcer.notNull (aCredentials, "Credentials");

    final IUserManager aUserMgr = PhotonSecurityManager.getUserMgr ();

    if (aCredentials.hasBasicAuth ())
    {
      final BasicAuthClientCredentials aBasicAuth = aCredentials.getBasicAuth ();
      final IUser aUser = aUserMgr.getUserOfLoginName (aBasicAuth.getUserName ());
      if (aUser == null || aUser.isDeleted ())
      {
        // Deleted users are handled like non-existing users
        LOGGER.warn ("Invalid login name provided: '" + aBasicAuth.getUserName () + "'");
        throw new SMPUnknownUserException (aBasicAuth.getUserName ());
      }
      if (!aUserMgr.areUserIDAndPasswordValid (aUser.getID (), aBasicAuth.getPassword ()))
      {
        LOGGER.warn ("Invalid password provided for '" + aBasicAuth.getUserName () + "'");
        throw new SMPUnauthorizedException ("Username and/or password are invalid!");
      }
      if (aUser.isDisabled ())
      {
        LOGGER.warn ("User '" + aBasicAuth.getUserName () + "' is disabled");
        throw new SMPUnauthorizedException ("User is disabled!");
      }

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("The provided BasicAuth credentials resolved to the user '" + aUser.getLoginName () + "'");

      return aUser;
    }

    if (aCredentials.hasBearerToken ())
    {
      final IUserTokenManager aUserTokenMgr = PhotonSecurityManager.getUserTokenMgr ();

      final String sTokenString = aCredentials.getBearerToken ();
      final IUserToken aUserToken = aUserTokenMgr.getUserTokenOfTokenString (sTokenString);
      if (aUserToken == null)
      {
        // Deleted users are handled like non-existing users
        LOGGER.warn ("Invalid Bearer token provided: '" + sTokenString + "'");
        throw new SMPUnknownUserException ("{BearerToken}" + sTokenString);
      }
      if (aUserToken.isDeleted ())
      {
        // Deleted tokens are handled like non-existing token
        LOGGER.warn ("Deleted Bearer token provided: '" + sTokenString + "'");
        throw new SMPUnknownUserException ("{BearerToken}" + sTokenString);
      }
      final IUser aUser = aUserToken.getUser ();
      if (aUser.isDeleted ())
      {
        // Deleted users are handled like non-existing users
        LOGGER.warn ("The user to which the Bearer token '" + sTokenString + "' belongs is deleted");
        throw new SMPUnknownUserException (aUser.getLoginName ());
      }
      if (aUser.isDisabled ())
      {
        LOGGER.warn ("User '" + aUser.getLoginName () + "' of Bearer token '" + sTokenString + "' is disabled");
        throw new SMPUnauthorizedException ("User is disabled!");
      }

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("The provided Bearer token resolved to the user '" + aUser.getLoginName () + "'");

      return aUser;
    }

    throw new IllegalStateException ("Unsupported credential method provided!");
  }

  public static void verifyOwnership (@NonNull final IParticipantIdentifier aServiceGroupID,
                                      @NonNull final IUser aCurrentUser) throws SMPNotFoundException,
                                                                         SMPUnauthorizedException
  {
    // Resolve service group
    final ISMPServiceGroup aServiceGroup = SMPMetaManager.getServiceGroupMgr ()
                                                         .getSMPServiceGroupOfID (aServiceGroupID);
    if (aServiceGroup == null)
    {
      throw new SMPNotFoundException ("Service group " + aServiceGroupID.getURIEncoded () + " does not exist");
    }

    // Resolve user
    final String sOwnerID = aServiceGroup.getOwnerID ();
    if (!sOwnerID.equals (aCurrentUser.getID ()))
    {
      throw new SMPUnauthorizedException ("User '" +
                                          aCurrentUser.getLoginName () +
                                          "' does not own " +
                                          aServiceGroupID.getURIEncoded ());
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Verified service group " +
                    aServiceGroup.getID () +
                    " is owned by user '" +
                    aCurrentUser.getLoginName () +
                    "'");
  }
}
