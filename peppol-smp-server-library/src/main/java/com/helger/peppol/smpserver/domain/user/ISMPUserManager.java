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
package com.helger.peppol.smpserver.domain.user;

import java.util.Collection;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.smpserver.exception.SMPNotFoundException;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;
import com.helger.web.http.basicauth.BasicAuthClientCredentials;

/**
 * Abstraction interface for the user management depending on the used backend.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public interface ISMPUserManager
{
  /**
   * @return <code>true</code> for SQL, <code>false</code> for XML. If this
   *         method returns <code>true</code> all user objects must implement
   *         {@link ISMPUserEditable}!
   */
  boolean isSpecialUserManagementNeeded ();

  void createUser (@Nonnull String sUserName, @Nonnull String sPassword);

  void updateUser (@Nonnull String sUserName, @Nonnull String sPassword);

  void deleteUser (@Nullable String sUserName);

  /**
   * @return The number of contained user. Always &ge; 0.
   */
  @Nonnegative
  int getUserCount ();

  @Nonnull
  @ReturnsMutableCopy
  Collection <? extends ISMPUser> getAllUsers ();

  @Nullable
  ISMPUser getUserOfID (String sUserID);

  /**
   * Check if an SMP user matching the user name of the BasicAuth credentials
   * exists, and that the passwords match. So this method verifies that the
   * BasicAuth credentials are valid.
   *
   * @param aCredentials
   *        The credentials to be validated. May not be <code>null</code>.
   * @return The matching non-<code>null</code> {@link ISMPUser}.
   * @throws Throwable
   *         If no user matching the passed user name is present or if the
   *         password in the credentials does not match the stored password
   *         (hash).
   */
  @Nonnull
  ISMPUser validateUserCredentials (@Nonnull BasicAuthClientCredentials aCredentials) throws Throwable;

  /**
   * Verify that the passed service group is owned by the user specified in the
   * credentials.
   *
   * @param aServiceGroupID
   *        The service group to be verified
   * @param aCurrentUser
   *        The user to verify.
   * @return Implementation specific return value.
   * @throws SMPNotFoundException
   *         If the passed service group does not exist on this SMP.
   * @throws SMPUnauthorizedException
   *         If the participant identifier is not owned by the user specified in
   *         the credentials
   */
  @Nullable
  Object verifyOwnership (@Nonnull final IParticipantIdentifier aServiceGroupID,
                          @Nonnull final ISMPUser aCurrentUser) throws SMPNotFoundException, SMPUnauthorizedException;
}
