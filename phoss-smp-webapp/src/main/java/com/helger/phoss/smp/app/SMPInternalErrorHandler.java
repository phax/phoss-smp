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
package com.helger.phoss.smp.app;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.email.EmailAddress;
import com.helger.commons.string.StringHelper;
import com.helger.config.IConfig;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.config.SMPConfigProvider;
import com.helger.photon.core.interror.InternalErrorBuilder;
import com.helger.photon.core.interror.InternalErrorSettings;
import com.helger.photon.core.interror.callback.AbstractErrorCallback;
import com.helger.smtp.settings.SMTPSettings;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class SMPInternalErrorHandler extends AbstractErrorCallback
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPInternalErrorHandler.class);

  @Nonnull
  public static InternalErrorBuilder createInternalErrorBuilder ()
  {
    final InternalErrorBuilder ret = new InternalErrorBuilder ();
    ret.setDisplayLocale (CSMPServer.DEFAULT_LOCALE);
    return ret;
  }

  @Override
  protected void onError (@Nullable final Throwable t,
                          @Nullable final IRequestWebScopeWithoutResponse aRequestScope,
                          @Nonnull @Nonempty final String sErrorCode,
                          @Nullable final Map <String, String> aCustomAttrs)
  {
    createInternalErrorBuilder ().setThrowable (t)
                                 .setRequestScope (aRequestScope)
                                 .addErrorMessage (sErrorCode)
                                 .addCustomData (aCustomAttrs)
                                 .handle ();
  }

  public static void doSetup ()
  {
    final IConfig aConfig = SMPConfigProvider.getConfig ();
    final String sSenderAddress = aConfig.getAsString ("smp.errorhandler.sender.email");
    final String sSenderName = aConfig.getAsString ("smp.errorhandler.sender.name", "SMP Internal Error Sender");
    final String sReceiverAddress = aConfig.getAsString ("smp.errorhandler.receiver.email");
    final String sReceiverName = aConfig.getAsString ("smp.errorhandler.receiver.name");
    final String sSMTPHostName = aConfig.getAsString ("smp.smtp.hostname");
    final int nSMTPPort = aConfig.getAsInt ("smp.smtp.port", -1);
    final String sSMTPUserName = aConfig.getAsString ("smp.smtp.username");
    final String sSMTPPassword = aConfig.getAsString ("smp.smtp.password");
    final boolean bSMTPSSLEnabled = aConfig.getAsBoolean ("smp.smtp.ssl", false);
    final boolean bSMTPSTARTTLSEnabled = aConfig.getAsBoolean ("smp.smtp.starttls", false);
    final long nSMTPConnectionTimeoutMS = aConfig.getAsLong ("smp.smtp.connectiontimeoutms", 10_000);
    final long nSMTPSocketTimeoutMS = aConfig.getAsLong ("smp.smtp.sockettimeoutms", 10_000);
    final boolean bSMTPDebug = aConfig.getAsBoolean ("smp.smtp.debug", false);
    final SMTPSettings aSMTPSettings = StringHelper.hasText (sSMTPHostName) ? new SMTPSettings (sSMTPHostName,
                                                                                                nSMTPPort,
                                                                                                sSMTPUserName,
                                                                                                sSMTPPassword,
                                                                                                StandardCharsets.UTF_8,
                                                                                                bSMTPSSLEnabled,
                                                                                                bSMTPSTARTTLSEnabled,
                                                                                                nSMTPConnectionTimeoutMS,
                                                                                                nSMTPSocketTimeoutMS,
                                                                                                bSMTPDebug)
                                                                            : null;
    if (StringHelper.hasText (sSenderAddress) &&
        StringHelper.hasText (sReceiverAddress) &&
        aSMTPSettings != null &&
        aSMTPSettings.areRequiredFieldsSet ())
    {
      // Set global internal error handlers
      new SMPInternalErrorHandler ().install ();

      InternalErrorSettings.setSMTPSenderAddress (new EmailAddress (sSenderAddress, sSenderName));
      InternalErrorSettings.setSMTPReceiverAddresses (new EmailAddress (sReceiverAddress, sReceiverName));
      InternalErrorSettings.setSMTPSettings (aSMTPSettings);
      InternalErrorSettings.setFallbackLocale (CSMPServer.DEFAULT_LOCALE);

      if (LOGGER.isInfoEnabled ())
        LOGGER.info ("Setup internal error handler to send emails on internal errors to " + sReceiverAddress);
    }
    else
    {
      LOGGER.info ("No internal error handler configuration was found. So not sending emails in case of error.");
    }
  }
}
