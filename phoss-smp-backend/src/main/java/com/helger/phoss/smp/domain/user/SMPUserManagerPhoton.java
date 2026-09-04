/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPUnauthorizedException;
import com.helger.phoss.smp.exception.SMPUnknownUserException;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.photon.security.login.RequestUserIDProvider;
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
  /**
   * The generic error message that is returned for all authentication failures, if
   * {@link SMPServerConfiguration#isRestAuthErrorDetails()} is disabled. It is deliberately the
   * same for all failure reasons, so that it cannot be determined from the outside, whether a
   * specific user exists or not.
   *
   * @since 8.2.0
   */
  public static final String MSG_AUTH_FAILED = "Username and/or password are invalid!";

  private static final Logger LOGGER = LoggerFactory.getLogger (SMPUserManagerPhoton.class);

  private SMPUserManagerPhoton ()
  {}

  /**
   * @param sDetails
   *        The detailed error message. May not be <code>null</code>.
   * @return The detailed error message, if the REST authentication error details are enabled, and
   *         {@link #MSG_AUTH_FAILED} otherwise.
   */
  @NonNull
  private static String _getAuthErrorMsg (@NonNull final String sDetails)
  {
    return SMPServerConfiguration.isRestAuthErrorDetails () ? sDetails : MSG_AUTH_FAILED;
  }

  /**
   * @param sToken
   *        The Bearer token to mask. May be <code>null</code>.
   * @return A shortened representation of the provided Bearer token, so that the token itself is
   *         never logged and never returned to the caller.
   */
  @NonNull
  private static String _getMaskedToken (@Nullable final String sToken)
  {
    final int nLength = StringHelper.getLength (sToken);
    if (nLength < 12)
    {
      // Too short to show anything of it
      return "*** (length " + nLength + ")";
    }
    return sToken.substring (0, 4) + "*** (length " + nLength + ")";
  }

  /**
   * Check if the provided credentials are valid. This checks if the user exists, if it is not
   * deleted, if the password matches and if the user is not disabled. If valid, the resolved user
   * is returned.
   *
   * @param aCredentials
   *        The credentials to check. May not be <code>null</code>.
   * @return <code>null</code> if something does wrong, the user on success only.
   * @throws SMPUnknownUserException
   *         if the user does not exist or if the user is marked as deleted.
   * @throws SMPUnauthorizedException
   *         If the password is invalid or if the user is marked as disabled
   * @see SMPServerConfiguration#isRestAuthErrorDetails() for controlling the amount of details in
   *      the error messages of the thrown exceptions
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
        throw new SMPUnknownUserException (aBasicAuth.getUserName (),
                                           _getAuthErrorMsg ("Unknown user '" + aBasicAuth.getUserName () + "'"));
      }
      if (!aUserMgr.areUserIDAndPasswordValid (aUser.getID (), aBasicAuth.getPassword ()))
      {
        LOGGER.warn ("Invalid password provided for '" + aBasicAuth.getUserName () + "'");
        throw new SMPUnauthorizedException (MSG_AUTH_FAILED);
      }
      if (aUser.isDisabled ())
      {
        LOGGER.warn ("User '" + aBasicAuth.getUserName () + "' is disabled");
        throw new SMPUnauthorizedException (_getAuthErrorMsg ("User is disabled!"));
      }

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("The provided BasicAuth credentials resolved to the user '" + aUser.getLoginName () + "'");

      RequestUserIDProvider.setCurrentUserID (aUser.getID ());
      return aUser;
    }

    if (aCredentials.hasBearerToken ())
    {
      final IUserTokenManager aUserTokenMgr = PhotonSecurityManager.getUserTokenMgr ();

      final String sTokenString = aCredentials.getBearerToken ();
      // Never log or return the Bearer token itself
      final String sMaskedToken = _getMaskedToken (sTokenString);
      final IUserToken aUserToken = aUserTokenMgr.getUserTokenOfTokenString (sTokenString);
      if (aUserToken == null)
      {
        // Deleted users are handled like non-existing users
        LOGGER.warn ("Invalid Bearer token provided: '" + sMaskedToken + "'");
        throw new SMPUnknownUserException ("{BearerToken}" + sMaskedToken,
                                           _getAuthErrorMsg ("Unknown Bearer token"));
      }
      if (aUserToken.isDeleted ())
      {
        // Deleted tokens are handled like non-existing token
        LOGGER.warn ("Deleted Bearer token provided: '" + sMaskedToken + "'");
        throw new SMPUnknownUserException ("{BearerToken}" + sMaskedToken,
                                           _getAuthErrorMsg ("Unknown Bearer token"));
      }
      final IUser aUser = aUserToken.getUser ();
      if (aUser.isDeleted ())
      {
        // Deleted users are handled like non-existing users
        LOGGER.warn ("The user to which the Bearer token '" + sMaskedToken + "' belongs is deleted");
        throw new SMPUnknownUserException (aUser.getLoginName (),
                                           _getAuthErrorMsg ("Unknown user '" + aUser.getLoginName () + "'"));
      }
      if (aUser.isDisabled ())
      {
        LOGGER.warn ("User '" + aUser.getLoginName () + "' of Bearer token '" + sMaskedToken + "' is disabled");
        throw new SMPUnauthorizedException (_getAuthErrorMsg ("User is disabled!"));
      }

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("The provided Bearer token resolved to the user '" + aUser.getLoginName () + "'");

      RequestUserIDProvider.setCurrentUserID (aUser.getID ());
      return aUser;
    }

    throw new IllegalStateException ("Unsupported credential method provided!");
  }

  /**
   * Verify that the Service Group is owned by the provided user. If the method returns normally,
   * the ownership is fine.
   * 
   * @param aServiceGroupID
   *        The service group ID to check. May not be <code>null</code>
   * @param aCurrentUser
   *        The user to check for ownership. May not be <code>null</code>
   * @throws SMPNotFoundException
   *         If no such service group exists
   * @throws SMPUnauthorizedException
   *         If the service group is owned by a different user.
   */
  public static void verifyOwnership (@NonNull final IParticipantIdentifier aServiceGroupID,
                                      @NonNull final IUser aCurrentUser) throws SMPNotFoundException,
                                                                         SMPUnauthorizedException
  {
    // Resolve service group
    final ISMPServiceGroup aServiceGroup = SMPMetaManager.getServiceGroupMgr ()
                                                         .getSMPServiceGroupOfID (aServiceGroupID);
    if (aServiceGroup == null)
    {
      throw SMPNotFoundException.unknownSG (aServiceGroupID.getURIEncoded (), null);
    }

    // Resolve user
    final String sOwnerID = aServiceGroup.getOwnerID ();
    if (!sOwnerID.equals (aCurrentUser.getID ()))
    {
      throw new SMPUnauthorizedException ("User '" +
                                          aCurrentUser.getLoginName () +
                                          "' does not own '" +
                                          aServiceGroup.getID () +
                                          "'");
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Verified service group " +
                    aServiceGroup.getID () +
                    " is owned by user '" +
                    aCurrentUser.getLoginName () +
                    "'");
  }
}
