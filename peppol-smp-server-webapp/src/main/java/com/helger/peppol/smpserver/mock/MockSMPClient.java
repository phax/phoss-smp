/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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
