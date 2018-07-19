/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.smlhook;

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
import com.helger.peppol.sml.ISMLInfo;
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
public class RegistrationHookWriteToSML implements IRegistrationHook
{
  private static final Logger LOGGER = LoggerFactory.getLogger (RegistrationHookWriteToSML.class);
  // SMP ID is static and cannot change
  private static final String s_sSMPID = SMPServerConfiguration.getSMLSMPID ();

  static
  {}

  public RegistrationHookWriteToSML ()
  {}

  @Nonnull
  private static ManageParticipantIdentifierServiceCaller _createSMLCaller ()
  {
    // SML endpoint (incl. the service name)
    final ISMLInfo aSMLInfo = SMPMetaManager.getSettings ().getSMLInfo ();
    if (aSMLInfo == null)
      throw new IllegalStateException ("Failed to get SML manage participant endpoint URL");
    final URL aSMLEndpointURL = aSMLInfo.getManageParticipantIdentifierEndpointAddress ();

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
    final Integer aConnectionTimeoutMS = SMPServerConfiguration.getSMLConnectionTimeoutMS ();
    if (aConnectionTimeoutMS != null)
      ret.setConnectionTimeoutMS (aConnectionTimeoutMS.intValue ());
    final Integer aRequestTimeoutMS = SMPServerConfiguration.getSMLRequestTimeoutMS ();
    if (aRequestTimeoutMS != null)
      ret.setRequestTimeoutMS (aRequestTimeoutMS.intValue ());
    return ret;
  }

  public void createServiceGroup (@Nonnull final IParticipantIdentifier aBusinessIdentifier) throws RegistrationHookException
  {
    final String sParticipantID = aBusinessIdentifier.getURIEncoded ();
    LOGGER.info ("Trying to CREATE business " + sParticipantID + " for " + s_sSMPID + " in SML");

    try
    {
      // Explicit constructor call is needed here!
      _createSMLCaller ().create (s_sSMPID, new SimpleParticipantIdentifier (aBusinessIdentifier));
      LOGGER.info ("Succeeded in CREATE business " + sParticipantID + " in SML");
    }
    catch (final UnauthorizedFault ex)
    {
      final String sMsg = "Seems like this SMP is not registered to the SML, or you're providing invalid credentials!";
      LOGGER.error (sMsg + " " + SMLExceptionHelper.getFaultMessage (ex));
      throw new RegistrationHookException (sMsg, ex);
    }
    catch (final Throwable t)
    {
      final String sMsg = "Could not create business " + sParticipantID + " in SML";
      LOGGER.error (sMsg, t);
      throw new RegistrationHookException (sMsg, t);
    }
  }

  public void undoCreateServiceGroup (@Nonnull final IParticipantIdentifier aBusinessIdentifier) throws RegistrationHookException
  {
    final String sParticipantID = aBusinessIdentifier.getURIEncoded ();
    LOGGER.warn ("CREATE failed in SMP backend, so deleting again business " +
                    sParticipantID +
                    " for " +
                    s_sSMPID +
                    " from SML.");

    try
    {
      // Undo create
      // Explicit constructor call is needed here!
      _createSMLCaller ().delete (s_sSMPID, new SimpleParticipantIdentifier (aBusinessIdentifier));
      LOGGER.warn ("Succeeded in deleting again business " + sParticipantID + " from SML.");
    }
    catch (final Throwable t)
    {
      final String sMsg = "Unable to rollback create business " + sParticipantID + " in SML";
      LOGGER.error (sMsg, t);
      throw new RegistrationHookException (sMsg, t);
    }
  }

  public void deleteServiceGroup (@Nonnull final IParticipantIdentifier aBusinessIdentifier) throws RegistrationHookException
  {
    final String sParticipantID = aBusinessIdentifier.getURIEncoded ();
    LOGGER.info ("Trying to DELETE business " + sParticipantID + " for " + s_sSMPID + " from SML");

    try
    {
      // Use the version with the SMP ID to be on the safe side
      // Explicit constructor call is needed here!
      _createSMLCaller ().delete (s_sSMPID, new SimpleParticipantIdentifier (aBusinessIdentifier));
      LOGGER.info ("Succeeded in deleting business " + sParticipantID + " from SML");
    }
    catch (final NotFoundFault ex)
    {
      final String sMsg = "The business " +
                          sParticipantID +
                          " was not present in the SML and hence could not be deleted.";
      LOGGER.error (sMsg, ex);
      throw new RegistrationHookException (sMsg, ex);
    }
    catch (final Throwable t)
    {
      final String sMsg = "Could not delete business " + sParticipantID + " from SML.";
      LOGGER.error (sMsg, t);
      throw new RegistrationHookException (sMsg, t);
    }
  }

  public void undoDeleteServiceGroup (@Nonnull final IParticipantIdentifier aBusinessIdentifier) throws RegistrationHookException
  {
    final String sParticipantID = aBusinessIdentifier.getURIEncoded ();
    LOGGER.warn ("DELETE failed in SMP backend, so creating again business " +
                    sParticipantID +
                    " for " +
                    s_sSMPID +
                    " in SML.");

    try
    {
      // Undo delete
      // Explicit constructor call is needed here!
      _createSMLCaller ().create (s_sSMPID, new SimpleParticipantIdentifier (aBusinessIdentifier));
      LOGGER.warn ("Succeeded in creating again business " + sParticipantID + " in SML.");
    }
    catch (final Throwable t)
    {
      final String sMsg = "Unable to rollback delete business " + sParticipantID + " in SML";
      LOGGER.error (sMsg, t);
      throw new RegistrationHookException (sMsg, t);
    }
  }
}
