package com.helger.peppol.smpserver.mock;

import com.helger.commons.url.URLHelper;
import com.helger.peppol.smpclient.SMPClient;

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
}
