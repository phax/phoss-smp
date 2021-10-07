/*
 * Copyright (C) 2015-2021 Philip Helger and contributors
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
import com.helger.phoss.smp.exception.SMPServerException;
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

  @Nonnull
  public static IUser validateUserCredentials (@Nonnull final BasicAuthClientCredentials aCredentials) throws SMPServerException
  {
    final IUserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
    final IUser aUser = aUserMgr.getUserOfLoginName (aCredentials.getUserName ());
    if (aUser == null)
    {
      LOGGER.info ("Invalid login name provided: '" + aCredentials.getUserName () + "'");
      throw new SMPUnknownUserException (aCredentials.getUserName ());
    }
    if (!aUserMgr.areUserIDAndPasswordValid (aUser.getID (), aCredentials.getPassword ()))
    {
      LOGGER.info ("Invalid password provided for '" + aCredentials.getUserName () + "'");
      throw new SMPUnauthorizedException ("Username and/or password are invalid!");
    }
    return aUser;
  }

  public static void verifyOwnership (@Nonnull final IParticipantIdentifier aServiceGroupID,
                                      @Nonnull final IUser aCurrentUser) throws SMPServerException
  {
    // Resolve service group
    final ISMPServiceGroup aServiceGroup = SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aServiceGroupID);
    if (aServiceGroup == null)
    {
      throw new SMPNotFoundException ("Service group " + aServiceGroupID.getURIEncoded () + " does not exist");
    }

    // Resolve user
    final String sOwnerID = aServiceGroup.getOwnerID ();
    if (!sOwnerID.equals (aCurrentUser.getID ()))
    {
      throw new SMPUnauthorizedException ("User '" + aCurrentUser.getLoginName () + "' does not own " + aServiceGroupID.getURIEncoded ());
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Verified service group " + aServiceGroup.getID () + " is owned by user '" + aCurrentUser.getLoginName () + "'");
  }
}
