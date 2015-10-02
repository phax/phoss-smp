/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.data.xml.mgr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.smpserver.domain.MetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.user.ISMPUser;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.domain.user.XMLDataUser;
import com.helger.peppol.smpserver.exception.SMPNotFoundException;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;
import com.helger.peppol.smpserver.exception.SMPUnknownUserException;
import com.helger.photon.basic.security.AccessManager;
import com.helger.photon.basic.security.user.IUser;
import com.helger.web.http.basicauth.BasicAuthClientCredentials;

/**
 * The DAO based {@link ISMPUserManager}.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class XMLUserManager implements ISMPUserManager
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (XMLUserManager.class);

  public XMLUserManager ()
  {}

  public boolean isSpecialUserManagementNeeded ()
  {
    return false;
  }

  public void createUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    // not needed
  }

  public void updateUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    // not needed
  }

  public void deleteUser (@Nonnull final String sUserName)
  {
    // not needed
  }

  @Nonnegative
  public int getUserCount ()
  {
    return AccessManager.getInstance ().getAllActiveUsers ().size ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <XMLDataUser> getAllUsers ()
  {
    final List <XMLDataUser> ret = new ArrayList <> ();
    for (final IUser aUser : AccessManager.getInstance ().getAllActiveUsers ())
      ret.add (new XMLDataUser (aUser));
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public XMLDataUser getUserOfID (@Nullable final String sUserID)
  {
    final IUser aUser = AccessManager.getInstance ().getUserOfID (sUserID);
    return aUser == null ? null : new XMLDataUser (aUser);
  }

  @Nonnull
  public XMLDataUser validateUserCredentials (@Nonnull final BasicAuthClientCredentials aCredentials) throws SMPUnauthorizedException,
                                                                                                      SMPUnknownUserException
  {
    final AccessManager aAccessMgr = AccessManager.getInstance ();
    final IUser aUser = aAccessMgr.getUserOfLoginName (aCredentials.getUserName ());
    if (aUser == null)
    {
      s_aLogger.info ("Invalid login name provided: '" + aCredentials.getUserName () + "'");
      throw new SMPUnknownUserException (aCredentials.getUserName ());
    }
    if (!aAccessMgr.areUserIDAndPasswordValid (aUser.getID (), aCredentials.getPassword ()))
    {
      s_aLogger.info ("Invalid password provided for '" + aCredentials.getUserName () + "'");
      throw new SMPUnauthorizedException ("Username and/or password are invalid!");
    }
    return new XMLDataUser (aUser);
  }

  @Nonnull
  public XMLDataUser createPreAuthenticatedUser (@Nonnull @Nonempty final String sUserName)
  {
    return new XMLDataUser (AccessManager.getInstance ().getUserOfLoginName (sUserName));
  }

  @Nonnull
  public ISMPServiceGroup verifyOwnership (@Nonnull final IParticipantIdentifier aServiceGroupID,
                                           @Nonnull final ISMPUser aCurrentUser) throws SMPNotFoundException,
                                                                                 SMPUnauthorizedException
  {
    // Resolve user group
    final ISMPServiceGroup aServiceGroup = MetaManager.getServiceGroupMgr ().getSMPServiceGroupOfID (aServiceGroupID);
    if (aServiceGroup == null)
    {
      throw new SMPNotFoundException ("Service group " +
                                      IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID) +
                                      " does not exist");
    }

    // Resolve user
    final String sOwnerID = aServiceGroup.getOwnerID ();
    if (!sOwnerID.equals (aCurrentUser.getID ()))
    {
      throw new SMPUnauthorizedException ("User '" +
                                          aCurrentUser.getUserName () +
                                          "' does not own " +
                                          IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID));
    }

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Verified service group " +
                       aServiceGroup.getID () +
                       " is owned by user '" +
                       aCurrentUser.getUserName () +
                       "'");

    return aServiceGroup;
  }
}
