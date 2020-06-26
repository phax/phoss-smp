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
package com.helger.phoss.smp.domain.user;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
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
 * Implementation {@link ISMPUserManager} using the build-in ph-oton user
 * management.
 *
 * @author Philip Helger
 */
public final class SMPUserManagerPhoton implements ISMPUserManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPUserManagerPhoton.class);

  public SMPUserManagerPhoton ()
  {}

  public boolean isSpecialUserManagementNeeded ()
  {
    return false;
  }

  @Nonnull
  public ESuccess createUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    // not needed
    // Success needed for tests
    return ESuccess.SUCCESS;
  }

  @Nonnull
  public ESuccess updateUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    // not needed
    // Success needed for tests
    return ESuccess.SUCCESS;
  }

  @Nonnull
  public EChange deleteUser (@Nullable final String sUserName)
  {
    // not needed
    return EChange.UNCHANGED;
  }

  @Nonnegative
  public long getUserCount ()
  {
    return PhotonSecurityManager.getUserMgr ().getActiveUserCount ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPUser> getAllUsers ()
  {
    final ICommonsList <ISMPUser> ret = new CommonsArrayList <> ();
    for (final IUser aUser : PhotonSecurityManager.getUserMgr ().getAllActiveUsers ())
      ret.add (new SMPUserPhoton (aUser));
    return ret;
  }

  @Nullable
  public SMPUserPhoton getUserOfID (@Nullable final String sUserID)
  {
    final IUser aUser = PhotonSecurityManager.getUserMgr ().getUserOfID (sUserID);
    return aUser == null ? null : new SMPUserPhoton (aUser);
  }

  @Nonnull
  public SMPUserPhoton validateUserCredentials (@Nonnull final BasicAuthClientCredentials aCredentials) throws SMPServerException
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
    return new SMPUserPhoton (aUser);
  }

  @Nonnull
  public SMPUserPhoton createPreAuthenticatedUser (@Nonnull @Nonempty final String sUserName)
  {
    final IUser aUser = PhotonSecurityManager.getUserMgr ().getUserOfLoginName (sUserName);
    if (aUser == null)
      throw new IllegalArgumentException ("Failed to resolve user of login name '" + sUserName + "'");
    return new SMPUserPhoton (aUser);
  }

  public void verifyOwnership (@Nonnull final IParticipantIdentifier aServiceGroupID,
                               @Nonnull final ISMPUser aCurrentUser) throws SMPServerException
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
      throw new SMPUnauthorizedException ("User '" + aCurrentUser.getUserName () + "' does not own " + aServiceGroupID.getURIEncoded ());
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Verified service group " + aServiceGroup.getID () + " is owned by user '" + aCurrentUser.getUserName () + "'");
  }
}
