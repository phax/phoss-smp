/**
 * Copyright (C) 2006-2016 BRZ GmbH
 * http://www.brz.gv.at
 *
 * All rights reserved
 */
package com.helger.peppol.smpserver.app;

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.quartz.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.charset.CCharset;
import com.helger.commons.email.EmailAddress;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.SMap;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.utils.ConfigFile;
import com.helger.photon.basic.longrun.ILongRunningJob;
import com.helger.photon.core.app.error.InternalErrorBuilder;
import com.helger.photon.core.app.error.InternalErrorHandler;
import com.helger.photon.core.app.error.callback.AbstractErrorCallback;
import com.helger.schedule.job.AbstractJob;
import com.helger.schedule.job.IJobExceptionCallback;
import com.helger.smtp.settings.SMTPSettings;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class SMPInternalErrorHandler extends AbstractErrorCallback implements IJobExceptionCallback
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SMPInternalErrorHandler.class);

  @Nonnull
  public static InternalErrorBuilder createInternalErrorBuilder ()
  {
    final InternalErrorBuilder ret = new InternalErrorBuilder ();
    return ret;
  }

  @Override
  protected void onError (@Nonnull final Throwable t,
                          @Nullable final IRequestWebScopeWithoutResponse aRequestScope,
                          @Nonnull @Nonempty final String sErrorCode,
                          @Nullable final Map <String, String> aCustomAttrs)
  {
    final Locale aDisplayLocale = getSafeDisplayLocale (CApp.DEFAULT_LOCALE);
    createInternalErrorBuilder ().setThrowable (t)
                                 .setRequestScope (aRequestScope)
                                 .addCustomData ("error code", sErrorCode)
                                 .addCustomData (aCustomAttrs)
                                 .setDisplayLocale (aDisplayLocale)
                                 .handle ();
  }

  public void onScheduledJobException (@Nonnull final Throwable t,
                                       @Nonnull final String sJobClassName,
                                       @Nonnull final Job aJob)
  {
    onError (t,
             null,
             "Error executing background Job " + sJobClassName,
             new SMap ().add ("job-class", sJobClassName)
                        .add ("job-object", aJob)
                        .add ("long-running", Boolean.toString (aJob instanceof ILongRunningJob)));
  }

  public static void doSetup ()
  {
    final ConfigFile aCF = SMPServerConfiguration.getConfigFile ();
    final String sSenderAddress = aCF.getString ("smp.errorhandler.sender.email");
    final String sSenderName = aCF.getString ("smp.errorhandler.sender.name", "SMP Internal Error Sender");
    final String sReceiverAddress = aCF.getString ("smp.errorhandler.receiver.email");
    final String sReceiverName = aCF.getString ("smp.errorhandler.receiver.name");
    final String sSMTPHostName = aCF.getString ("smp.smtp.hostname");
    final int nSMTPPort = aCF.getInt ("smp.smtp.port", -1);
    final String sSMTPUserName = aCF.getString ("smp.smtp.username");
    final String sSMTPPassword = aCF.getString ("smp.smtp.password");
    final boolean bSMTPSSLEnabled = aCF.getBoolean ("smp.smtp.ssl", false);
    final boolean bSMTPSTARTTLSEnabled = aCF.getBoolean ("smp.smtp.starttls", false);
    final long nSMTPConnectionTimeoutMS = aCF.getLong ("smp.smtp.connectiontimeoutms", 10_000);
    final long nSMTPSocketTimeoutMS = aCF.getLong ("smp.smtp.sockettimeoutms", 10_000);
    final boolean bSMTPDebug = aCF.getBoolean ("smp.smtp.debug", false);
    final SMTPSettings aSMTPSettings = StringHelper.hasText (sSMTPHostName) ? new SMTPSettings (sSMTPHostName,
                                                                                                nSMTPPort,
                                                                                                sSMTPUserName,
                                                                                                sSMTPPassword,
                                                                                                CCharset.CHARSET_UTF_8_OBJ,
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
      final SMPInternalErrorHandler aIntErrHdl = new SMPInternalErrorHandler ();
      AbstractErrorCallback.install (aIntErrHdl);
      AbstractJob.getExceptionCallbacks ().addCallback (aIntErrHdl);

      InternalErrorHandler.setSMTPSenderAddress (new EmailAddress (sSenderAddress, sSenderName));
      InternalErrorHandler.setSMTPReceiverAddresses (new EmailAddress (sReceiverAddress, sReceiverName));
      InternalErrorHandler.setSMTPSettings (aSMTPSettings);
      s_aLogger.info ("Setup internal error handler to send emails on internal errors to " + sReceiverAddress);
    }
  }
}
