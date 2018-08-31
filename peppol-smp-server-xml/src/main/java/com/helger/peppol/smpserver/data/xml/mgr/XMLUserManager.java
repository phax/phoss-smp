/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.data.xml.mgr;

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
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.data.xml.domain.XMLDataUser;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.user.ISMPUser;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.exception.SMPNotFoundException;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;
import com.helger.peppol.smpserver.exception.SMPUnknownUserException;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.UserManager;

/**
 * The DAO based {@link ISMPUserManager}.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class XMLUserManager implements ISMPUserManager
{
  private static final Logger LOGGER = LoggerFactory.getLogger (XMLUserManager.class);

  public XMLUserManager ()
  {}

  public boolean isSpecialUserManagementNeeded ()
  {
    return false;
  }

  @Nonnull
  public ESuccess createUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    // not needed
    return ESuccess.FAILURE;
  }

  @Nonnull
  public ESuccess updateUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    // not needed
    return ESuccess.FAILURE;
  }

  @Nonnull
  public EChange deleteUser (@Nullable final String sUserName)
  {
    // not needed
    return EChange.UNCHANGED;
  }

  @Nonnegative
  public int getUserCount ()
  {
    return PhotonSecurityManager.getUserMgr ().getActiveUserCount ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPUser> getAllUsers ()
  {
    final ICommonsList <ISMPUser> ret = new CommonsArrayList <> ();
    for (final IUser aUser : PhotonSecurityManager.getUserMgr ().getAllActiveUsers ())
      ret.add (new XMLDataUser (aUser));
    return ret;
  }

  @Nullable
  public XMLDataUser getUserOfID (@Nullable final String sUserID)
  {
    final IUser aUser = PhotonSecurityManager.getUserMgr ().getUserOfID (sUserID);
    return aUser == null ? null : new XMLDataUser (aUser);
  }

  @Nonnull
  public XMLDataUser validateUserCredentials (@Nonnull final BasicAuthClientCredentials aCredentials)
  {
    final UserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
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
    return new XMLDataUser (aUser);
  }

  @Nonnull
  public XMLDataUser createPreAuthenticatedUser (@Nonnull @Nonempty final String sUserName)
  {
    return new XMLDataUser (PhotonSecurityManager.getUserMgr ().getUserOfLoginName (sUserName));
  }

  @Nonnull
  public ISMPServiceGroup verifyOwnership (@Nonnull final IParticipantIdentifier aServiceGroupID,
                                           @Nonnull final ISMPUser aCurrentUser)
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
                                          aCurrentUser.getUserName () +
                                          "' does not own " +
                                          aServiceGroupID.getURIEncoded ());
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Verified service group " +
                    aServiceGroup.getID () +
                    " is owned by user '" +
                    aCurrentUser.getUserName () +
                    "'");

    return aServiceGroup;
  }
}
