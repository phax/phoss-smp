/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
import java.security.KeyStore;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.exception.InitializationException;
import com.helger.commons.random.VerySecureRandom;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smlclient.ManageParticipantIdentifierServiceCaller;
import com.helger.peppol.smlclient.participant.NotFoundFault;
import com.helger.peppol.smlclient.participant.UnauthorizedFault;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.utils.KeyStoreHelper;
import com.helger.web.https.DoNothingTrustManager;
import com.helger.web.https.HostnameVerifierAlwaysTrue;

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
  private static final URL s_aSMLEndpointURL;
  private static final String s_sSMPID;
  private static final SSLSocketFactory s_aDefaultSocketFactory;
  private static final HostnameVerifier s_aDefaultHostnameVerifier;

  static
  {
    // SML endpoint (incl. the service name)
    final String sSMLURL = SMPServerConfiguration.getSMLURL ();
    try
    {
      s_aSMLEndpointURL = new URL (sSMLURL);
    }
    catch (final MalformedURLException ex)
    {
      throw new InitializationException ("Failed to init SML endpoint URL from '" + sSMLURL + "'", ex);
    }
    s_aLogger.info ("Using the following SML address: " + s_aSMLEndpointURL);

    // SMP ID
    s_sSMPID = SMPServerConfiguration.getSMLSMPID ();
    s_aLogger.info ("This SMP has the ID: " + s_sSMPID);

    try
    {
      final String sKeystorePath = SMPServerConfiguration.getKeystorePath ();
      final String sKeystorePassword = SMPServerConfiguration.getKeystorePassword ();

      // Main key storage
      final KeyStore aKeyStore = KeyStoreHelper.loadKeyStore (sKeystorePath, sKeystorePassword);

      // Key manager
      final KeyManagerFactory aKeyManagerFactory = KeyManagerFactory.getInstance ("SunX509");
      aKeyManagerFactory.init (aKeyStore, sKeystorePassword.toCharArray ());

      // Trust manager

      // Assign key manager and empty trust manager to SSL/TLS context
      final SSLContext aSSLCtx = SSLContext.getInstance ("TLS");
      aSSLCtx.init (aKeyManagerFactory.getKeyManagers (), new TrustManager [] { new DoNothingTrustManager () }, VerySecureRandom.getInstance ());

      s_aDefaultSocketFactory = aSSLCtx.getSocketFactory ();
      if (s_aSMLEndpointURL.toExternalForm ().contains ("localhost"))
        s_aDefaultHostnameVerifier = new HostnameVerifierAlwaysTrue ();
      else
        s_aDefaultHostnameVerifier = null;
    }
    catch (final Exception ex)
    {
      throw new InitializationException ("Failed to init keyStore for SML access", ex);
    }
  }

  public RegistrationHookWriteToSML ()
  {}

  @Nonnull
  private ManageParticipantIdentifierServiceCaller _createSMLCaller ()
  {
    final ManageParticipantIdentifierServiceCaller ret = new ManageParticipantIdentifierServiceCaller (s_aSMLEndpointURL);
    ret.setSSLSocketFactory (s_aDefaultSocketFactory);
    ret.setHostnameVerifier (s_aDefaultHostnameVerifier);
    return ret;
  }

  public void createServiceGroup (@Nonnull final IParticipantIdentifier aBusinessIdentifier) throws RegistrationHookException
  {
    final String sParticipantID = IdentifierHelper.getIdentifierURIEncoded (aBusinessIdentifier);
    s_aLogger.info ("Trying to CREATE business " + sParticipantID + " for " + s_sSMPID + " in SML");

    try
    {
      _createSMLCaller ().create (s_sSMPID, new SimpleParticipantIdentifier (aBusinessIdentifier));
      s_aLogger.info ("Succeeded in CREATE business " + sParticipantID + " in SML");
    }
    catch (final UnauthorizedFault ex)
    {
      final String sMsg = "Seems like this SMP is not registered to the SML, or you're providing invalid credentials!";
      s_aLogger.error (sMsg);
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
    final String sParticipantID = IdentifierHelper.getIdentifierURIEncoded (aBusinessIdentifier);
    s_aLogger.warn ("CREATE failed in SMP backend, so deleting again business " + sParticipantID + " for " + s_sSMPID + " from SML.");

    try
    {
      // Undo create
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
    final String sParticipantID = IdentifierHelper.getIdentifierURIEncoded (aBusinessIdentifier);
    s_aLogger.info ("Trying to DELETE business " + sParticipantID + " for " + s_sSMPID + " from SML");

    try
    {
      // Use the version with the SMP ID to be on the safe side
      _createSMLCaller ().delete (s_sSMPID, new SimpleParticipantIdentifier (aBusinessIdentifier));
      s_aLogger.info ("Succeeded in deleting business " + sParticipantID + " from SML");
    }
    catch (final NotFoundFault ex)
    {
      final String sMsg = "The business " + sParticipantID + " was not present in the SML and hence could not be deleted.";
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
    final String sParticipantID = IdentifierHelper.getIdentifierURIEncoded (aBusinessIdentifier);
    s_aLogger.warn ("DELETE failed in SMP backend, so creating again business " + sParticipantID + " for " + s_sSMPID + " in SML.");

    try
    {
      // Undo delete
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
