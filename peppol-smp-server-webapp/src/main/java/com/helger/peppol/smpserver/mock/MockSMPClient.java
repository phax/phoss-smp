package com.helger.peppol.smpserver.mock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.url.URLHelper;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smp.ServiceGroupType;
import com.helger.peppol.smpclient.SMPClient;
import com.helger.peppol.smpclient.exception.SMPClientException;

/**
 * A special SMP client customized for testing purposes only.
 *
 * @author Philip Helger
 */
public class MockSMPClient extends SMPClient
{
  public MockSMPClient ()
  {
    super (URLHelper.getAsURI (MockWebServer.BASE_URI_HTTP));
  }

  @Override
  @Nullable
  public ServiceGroupType getServiceGroupOrNull (@Nonnull final IParticipantIdentifier aServiceGroupID) throws SMPClientException
  {
    try
    {
      return super.getServiceGroup (aServiceGroupID);
    }
    catch (final SMPClientException ex)
    {
      return null;
    }
  }

  @Override
  public void deleteServiceGroup (@Nonnull final IParticipantIdentifier aServiceGroupID,
                                  @Nonnull final BasicAuthClientCredentials aCredentials)
  {
    try
    {
      super.deleteServiceGroup (aServiceGroupID, aCredentials);
    }
    catch (final SMPClientException ex)
    {
      // Ignore
    }
  }
}
