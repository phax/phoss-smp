package com.helger.peppol.smpserver.mock;

import com.helger.commons.collection.ext.ICommonsCollection;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.domain.user.ISMPUser;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.exception.SMPNotFoundException;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;
import com.helger.web.http.basicauth.BasicAuthClientCredentials;

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

  public ICommonsCollection <? extends ISMPUser> getAllUsers ()
  {
    throw new UnsupportedOperationException ();
  }

  public void deleteUser (final String sUserName)
  {}

  public void createUser (final String sUserName, final String sPassword)
  {}
}
