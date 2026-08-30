/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.servlet;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.VisibleForTesting;
import com.helger.http.CHttp;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.mime.CMimeType;
import com.helger.mime.MimeType;
import com.helger.phoss.smp.ready.SMPReadyProvider;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

/**
 * Create the backend-aware readiness response as a small JSON object.
 *
 * @author vinit-thummar
 * @since 8.3.1
 */
public class SMPReadyXServletHandler implements IXServletSimpleHandler
{
  private static final Charset CHARSET = StandardCharsets.UTF_8;

  /**
   * Fill the provided response with the readiness state: HTTP 200 if the SMP is ready and HTTP 503
   * if it is not. In both cases a small JSON object is returned as the payload.
   *
   * @param bReady
   *        The readiness state to be returned.
   * @param aUnifiedResponse
   *        The response to be filled. May not be <code>null</code>.
   */
  @VisibleForTesting
  static void fillResponse (final boolean bReady, @NonNull final UnifiedResponse aUnifiedResponse)
  {
    final IJsonObject aData = new JsonObject ();
    aData.add ("ready", bReady);

    aUnifiedResponse.disableCaching ();
    aUnifiedResponse.setMimeType (new MimeType (CMimeType.APPLICATION_JSON).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                                          CHARSET.name ()));
    if (!bReady)
    {
      aUnifiedResponse.setAllowContentOnStatusCode (true);
      aUnifiedResponse.setStatus (CHttp.HTTP_SERVICE_UNAVAILABLE);
    }
    aUnifiedResponse.setContentAndCharset (aData.getAsJsonString (), CHARSET);
  }

  public void handleRequest (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                             @NonNull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    fillResponse (SMPReadyProvider.isReady (), aUnifiedResponse);
  }
}
