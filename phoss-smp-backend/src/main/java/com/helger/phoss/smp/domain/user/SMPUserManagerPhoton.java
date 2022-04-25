/*
 * Copyright (C) 2015-2022 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.user;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPUnauthorizedException;
import com.helger.phoss.smp.exception.SMPUnknownUserException;
import com.helger.photon.security.mgr.PhotonSecurityManager;
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
  @Nonnull
  public static IUser validateUserCredentials (@Nonnull final BasicAuthClientCredentials aCredentials) throws SMPUnknownUserException,
                                                                                                       SMPUnauthorizedException
  {
    final IUserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
    final IUser aUser = aUserMgr.getUserOfLoginName (aCredentials.getUserName ());
    if (aUser == null || aUser.isDeleted ())
    {
      // Deleted users are handled like non-existing users
      LOGGER.warn ("Invalid login name provided: '" + aCredentials.getUserName () + "'");
      throw new SMPUnknownUserException (aCredentials.getUserName ());
    }
    if (!aUserMgr.areUserIDAndPasswordValid (aUser.getID (), aCredentials.getPassword ()))
    {
      LOGGER.warn ("Invalid password provided for '" + aCredentials.getUserName () + "'");
      throw new SMPUnauthorizedException ("Username and/or password are invalid!");
    }
    if (aUser.isDisabled ())
    {
      LOGGER.warn ("User '" + aCredentials.getUserName () + "' is disabled");
      throw new SMPUnauthorizedException ("User is disabled!");
    }
    return aUser;
  }

  public static void verifyOwnership (@Nonnull final IParticipantIdentifier aServiceGroupID,
                                      @Nonnull final IUser aCurrentUser) throws SMPNotFoundException,
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
