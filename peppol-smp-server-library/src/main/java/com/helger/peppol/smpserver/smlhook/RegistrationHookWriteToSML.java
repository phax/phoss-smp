/**
 * Copyright (C) 2015-2017 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.smlhook;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.exception.InitializationException;
import com.helger.commons.ws.HostnameVerifierVerifyAll;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smlclient.ManageParticipantIdentifierServiceCaller;
import com.helger.peppol.smlclient.SMLExceptionHelper;
import com.helger.peppol.smlclient.participant.NotFoundFault;
import com.helger.peppol.smlclient.participant.UnauthorizedFault;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.security.SMPKeyManager;

/**
 * An implementation of the RegistrationHook that informs the SML of updates to
 * this SMP's identifiers.<br>
 * The design of this hook is very bogus! It relies on the postUpdate always
 * being called in order in this Thread.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@NotThreadSafe
public final class RegistrationHookWriteToSML implements IRegistrationHook
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (RegistrationHookWriteToSML.class);
  // SMP ID is static and cannot change
  private static final String s_sSMPID = SMPServerConfiguration.getSMLSMPID ();

  static
  {}

  public RegistrationHookWriteToSML ()
  {}

  @Nonnull
  private ManageParticipantIdentifierServiceCaller _createSMLCaller ()
  {
    // SML endpoint (incl. the service name)
    final String sSMLURL = SMPMetaManager.getSettings ().getSMLURL ();
    URL aSMLEndpointURL;
    try
    {
      aSMLEndpointURL = new URL (sSMLURL);
    }
    catch (final MalformedURLException ex)
    {
      throw new IllegalStateException ("Failed to init SML endpoint URL from '" + sSMLURL + "'", ex);
    }

    // SSL socket factory
    SSLSocketFactory aSocketFactory;
    if ("https".equals (aSMLEndpointURL.getProtocol ()))
    {
      // https connection
      if (!SMPKeyManager.isCertificateValid ())
        throw new InitializationException ("Cannot init registration hook to SML, because private key/certificate setup has errors: " +
                                           SMPKeyManager.getInitializationError ());

      try
      {
        aSocketFactory = SMPKeyManager.getInstance ().createSSLContext ().getSocketFactory ();
      }
      catch (final Exception ex)
      {
        throw new IllegalStateException ("Failed to init SSLContext for SML access", ex);
      }
    }
    else
    {
      // Local, http only access - no socket factory
      aSocketFactory = null;
    }

    // Hostname verifier
    HostnameVerifier aHostnameVerifier;
    if (aSMLEndpointURL.toExternalForm ().toLowerCase (Locale.US).contains ("localhost"))
      aHostnameVerifier = new HostnameVerifierVerifyAll ();
    else
      aHostnameVerifier = null;

    // Build WS client
    final ManageParticipantIdentifierServiceCaller ret = new ManageParticipantIdentifierServiceCaller (aSMLEndpointURL);
    ret.setSSLSocketFactory (aSocketFactory);
    ret.setHostnameVerifier (aHostnameVerifier);
    return ret;
  }

  public void createServiceGroup (@Nonnull final IParticipantIdentifier aBusinessIdentifier) throws RegistrationHookException
  {
    final String sParticipantID = aBusinessIdentifier.getURIEncoded ();
    s_aLogger.info ("Trying to CREATE business " + sParticipantID + " for " + s_sSMPID + " in SML");

    try
    {
      // Explicit constructor call is needed here!
      _createSMLCaller ().create (s_sSMPID, new SimpleParticipantIdentifier (aBusinessIdentifier));
      s_aLogger.info ("Succeeded in CREATE business " + sParticipantID + " in SML");
    }
    catch (final UnauthorizedFault ex)
    {
      final String sMsg = "Seems like this SMP is not registered to the SML, or you're providing invalid credentials!";
      s_aLogger.error (sMsg + " " + SMLExceptionHelper.getFaultMessage (ex));
      throw new RegistrationHookException (sMsg, ex);
    }
    catch (final Throwable t)
    {
      final String sMsg = "Could not create business " + sParticipantID + " in SML";
      s_aLogger.error (sMsg, t);
      throw new RegistrationHookException (sMsg, t);
    }
  }

  public void undoCreateServiceGroup (@Nonnull final IParticipantIdentifier aBusinessIdentifier) throws RegistrationHookException
  {
    final String sParticipantID = aBusinessIdentifier.getURIEncoded ();
    s_aLogger.warn ("CREATE failed in SMP backend, so deleting again business " +
                    sParticipantID +
                    " for " +
                    s_sSMPID +
                    " from SML.");

    try
    {
      // Undo create
      // Explicit constructor call is needed here!
      _createSMLCaller ().delete (s_sSMPID, new SimpleParticipantIdentifier (aBusinessIdentifier));
      s_aLogger.warn ("Succeeded in deleting again business " + sParticipantID + " from SML.");
    }
    catch (final Throwable t)
    {
      final String sMsg = "Unable to rollback create business " + sParticipantID + " in SML";
      s_aLogger.error (sMsg, t);
      throw new RegistrationHookException (sMsg, t);
    }
  }

  public void deleteServiceGroup (@Nonnull final IParticipantIdentifier aBusinessIdentifier) throws RegistrationHookException
  {
    final String sParticipantID = aBusinessIdentifier.getURIEncoded ();
    s_aLogger.info ("Trying to DELETE business " + sParticipantID + " for " + s_sSMPID + " from SML");

    try
    {
      // Use the version with the SMP ID to be on the safe side
      // Explicit constructor call is needed here!
      _createSMLCaller ().delete (s_sSMPID, new SimpleParticipantIdentifier (aBusinessIdentifier));
      s_aLogger.info ("Succeeded in deleting business " + sParticipantID + " from SML");
    }
    catch (final NotFoundFault ex)
    {
      final String sMsg = "The business " +
                          sParticipantID +
                          " was not present in the SML and hence could not be deleted.";
      s_aLogger.error (sMsg, ex);
      throw new RegistrationHookException (sMsg, ex);
    }
    catch (final Throwable t)
    {
      final String sMsg = "Could not delete business " + sParticipantID + " from SML.";
      s_aLogger.error (sMsg, t);
      throw new RegistrationHookException (sMsg, t);
    }
  }

  public void undoDeleteServiceGroup (@Nonnull final IParticipantIdentifier aBusinessIdentifier) throws RegistrationHookException
  {
    final String sParticipantID = aBusinessIdentifier.getURIEncoded ();
    s_aLogger.warn ("DELETE failed in SMP backend, so creating again business " +
                    sParticipantID +
                    " for " +
                    s_sSMPID +
                    " in SML.");

    try
    {
      // Undo delete
      // Explicit constructor call is needed here!
      _createSMLCaller ().create (s_sSMPID, new SimpleParticipantIdentifier (aBusinessIdentifier));
      s_aLogger.warn ("Succeeded in creating again business " + sParticipantID + " in SML.");
    }
    catch (final Throwable t)
    {
      final String sMsg = "Unable to rollback delete business " + sParticipantID + " in SML";
      s_aLogger.error (sMsg, t);
      throw new RegistrationHookException (sMsg, t);
    }
  }
}
