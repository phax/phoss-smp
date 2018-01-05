/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.mock;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.domain.user.ISMPUser;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.exception.SMPNotFoundException;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;

/**
 * Mock implementation of {@link ISMPUserManager}.
 *
 * @author Philip Helger
 */
final class MockSMPUserManager implements ISMPUserManager
{
  public Object verifyOwnership (final IParticipantIdentifier aServiceGroupID,
                                 final ISMPUser aCurrentUser) throws SMPNotFoundException, SMPUnauthorizedException
  {
    throw new UnsupportedOperationException ();
  }

  public ISMPUser validateUserCredentials (final BasicAuthClientCredentials aCredentials) throws Throwable
  {
    throw new UnsupportedOperationException ();
  }

  public void updateUser (final String sUserName, final String sPassword)
  {}

  public boolean isSpecialUserManagementNeeded ()
  {
    return false;
  }

  public ISMPUser getUserOfID (final String sUserID)
  {
    throw new UnsupportedOperationException ();
  }

  public int getUserCount ()
  {
    return 0;
  }

  public ICommonsList <ISMPUser> getAllUsers ()
  {
    throw new UnsupportedOperationException ();
  }

  public void deleteUser (final String sUserName)
  {}

  public void createUser (final String sUserName, final String sPassword)
  {}
}
