/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
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

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.mime.CMimeType;
import com.helger.commons.mime.MimeType;
import com.helger.json.IJsonObject;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.status.SMPStatusProvider;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.handler.simple.IXServletSimpleHandler;

/**
 * Create SMP status as JSON object. See
 * https://github.com/phax/phoss-smp/wiki/Status-API for details.
 *
 * @author Philip Helger
 * @since 5.0.6
 */
public class SMPStatusXServletHandler implements IXServletSimpleHandler
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPStatusXServletHandler.class);
  private static final Charset CHARSET = StandardCharsets.UTF_8;

  public void handleRequest (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                             @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Status information requested");

    // Build data to provide
    final IJsonObject aStatusData;
    if (SMPServerConfiguration.isStatusEnabled ())
    {
      // Special boolean parameter to ensure status works as health check
      final boolean bDisableLongRunningOperations = aRequestScope.params ().getAsBoolean ("disable-long-running", false);
      aStatusData = SMPStatusProvider.getDefaultStatusData (bDisableLongRunningOperations);
    }
    else
    {
      // Status is disabled in the configuration
      aStatusData = SMPStatusProvider.getStatusDisabledData ();
    }

    // Put JSON on response
    aUnifiedResponse.disableCaching ();
    aUnifiedResponse.setMimeType (new MimeType (CMimeType.APPLICATION_JSON).addParameter (CMimeType.PARAMETER_NAME_CHARSET,
                                                                                          CHARSET.name ()));
    aUnifiedResponse.setContentAndCharset (aStatusData.getAsJsonString (), CHARSET);
  }
}
