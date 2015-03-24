/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.random.VerySecureRandom;
import com.helger.commons.state.ESuccess;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smlclient.ManageParticipantIdentifierServiceCaller;
import com.helger.peppol.smlclient.participant.NotFoundFault;
import com.helger.peppol.smlclient.participant.UnauthorizedFault;
import com.helger.peppol.smpserver.CSMPServer;
import com.helger.peppol.utils.KeyStoreUtils;
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
  private static enum EAction
  {
    CREATE,
    DELETE
  }

  private static final String CONFIG_HOOK_REG_LOCATOR_URL = "regServiceRegistrationHook.regLocatorUrl";
  private static final String CONFIG_HOOK_ID = "regServiceRegistrationHook.id";
  private static final String CONFIG_HOOK_KEYSTORE_CLASSPATH = "regServiceRegistrationHook.keystore.classpath";
  private static final String CONFIG_HOOK_KEYSTORE_PASSWORD = "regServiceRegistrationHook.keystore.password";

  private static final Logger s_aLogger = LoggerFactory.getLogger (RegistrationHookWriteToSML.class);
  private static final URL s_aSMLEndpointURL;
  private static final String s_sSMPID;
  private static final ThreadLocal <IRegistrationHook> s_aPostUpdateCallback = new ThreadLocal <IRegistrationHook> ();

  static
  {

    // SML endpoint (incl. the service name)
    final String sURL = CSMPServer.getConfigFile ().getString (CONFIG_HOOK_REG_LOCATOR_URL);
    try
    {
      s_aSMLEndpointURL = new URL (sURL);
    }
    catch (final MalformedURLException ex)
    {
      throw new IllegalStateException ("Failed to init SML endpoint URL from '" + sURL + "'", ex);
    }

    // SMP ID
    s_sSMPID = CSMPServer.getConfigFile ().getString (CONFIG_HOOK_ID);

    s_aLogger.info ("Using the following SML address: " + s_aSMLEndpointURL);
    s_aLogger.info ("This SMP has the ID: " + s_sSMPID);
  }

  private SimpleParticipantIdentifier m_aBusinessIdentifier;
  private EAction m_eAction;

  public RegistrationHookWriteToSML ()
  {}

  @Nullable
  public static final IRegistrationHook getAndRemoveHook ()
  {
    final IRegistrationHook ret = s_aPostUpdateCallback.get ();
    s_aPostUpdateCallback.remove ();
    return ret;
  }

  private static void _setupSSLSocketFactory ()
  {
    // Keystore for SML access:
    try
    {
      final String sKeystorePath = CSMPServer.getConfigFile ().getString (CONFIG_HOOK_KEYSTORE_CLASSPATH);
      final String sKeystorePassword = CSMPServer.getConfigFile ().getString (CONFIG_HOOK_KEYSTORE_PASSWORD);
      final String sRegLocatorUrl = CSMPServer.getConfigFile ().getString (CONFIG_HOOK_REG_LOCATOR_URL);

      // Main key storage
      final KeyStore aKeyStore = KeyStoreUtils.loadKeyStore (sKeystorePath, sKeystorePassword);

      // Key manager
      final KeyManagerFactory aKeyManagerFactory = KeyManagerFactory.getInstance ("SunX509");
      aKeyManagerFactory.init (aKeyStore, sKeystorePassword.toCharArray ());

      // Trust manager

      // Assign key manager and empty trust manager to SSL/TLS context
      final SSLContext aSSLCtx = SSLContext.getInstance ("TLS");
      aSSLCtx.init (aKeyManagerFactory.getKeyManagers (),
                    new TrustManager [] { new DoNothingTrustManager () },
                    VerySecureRandom.getInstance ());
      HttpsURLConnection.setDefaultSSLSocketFactory (aSSLCtx.getSocketFactory ());
      if (sRegLocatorUrl.contains ("localhost"))
      {
        HttpsURLConnection.setDefaultHostnameVerifier (new HostnameVerifierAlwaysTrue ());
      }
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to init keyStore for SML access", ex);
    }
  }

  public void create (final IParticipantIdentifier aPI) throws HookException
  {
    m_aBusinessIdentifier = new SimpleParticipantIdentifier (aPI);
    m_eAction = EAction.CREATE;
    s_aLogger.info ("Trying to create business " + m_aBusinessIdentifier + " in Business Identifier Manager Service");

    try
    {
      _setupSSLSocketFactory ();
      final ManageParticipantIdentifierServiceCaller aSMLCaller = new ManageParticipantIdentifierServiceCaller (s_aSMLEndpointURL);
      aSMLCaller.create (s_sSMPID, m_aBusinessIdentifier);
      s_aLogger.info ("Succeeded in creating business " +
                      m_aBusinessIdentifier +
                      " using Business Identifier Manager Service");
      s_aPostUpdateCallback.set (this);
    }
    catch (final UnauthorizedFault ex)
    {
      final String sMsg = "Seems like this SMP is not registered to the SML, or you're providing invalid credentials!";
      s_aLogger.warn (sMsg);
      throw new HookException (sMsg, ex);
    }
    catch (final Throwable t)
    {
      final String sMsg = "Could not create business " +
                          m_aBusinessIdentifier +
                          " in Business Identifier Manager Service";
      s_aLogger.warn (sMsg, t);
      throw new HookException (sMsg, t);
    }
  }

  public void delete (final IParticipantIdentifier aPI) throws HookException
  {
    m_aBusinessIdentifier = new SimpleParticipantIdentifier (aPI);
    m_eAction = EAction.DELETE;
    s_aLogger.info ("Trying to delete business " + m_aBusinessIdentifier + " in Business Identifier Manager Service");

    try
    {
      _setupSSLSocketFactory ();
      final ManageParticipantIdentifierServiceCaller aSMPCaller = new ManageParticipantIdentifierServiceCaller (s_aSMLEndpointURL);
      aSMPCaller.delete (m_aBusinessIdentifier);
      s_aLogger.info ("Succeded in deleting business " +
                      m_aBusinessIdentifier +
                      " using Business Identifier Manager Service");
      s_aPostUpdateCallback.set (this);
    }
    catch (final NotFoundFault e)
    {
      s_aLogger.warn ("The business " + m_aBusinessIdentifier + " was not present in the SML. Just ignore.");
    }
    catch (final Throwable t)
    {
      final String sMsg = "Could not delete business " +
                          m_aBusinessIdentifier +
                          " in Business Identifier Manager Service";
      s_aLogger.warn (sMsg, t);
      throw new HookException (sMsg, t);
    }
  }

  public void postUpdate (final ESuccess eSuccess) throws HookException
  {
    if (eSuccess.isFailure ())
      try
      {
        _setupSSLSocketFactory ();
        final ManageParticipantIdentifierServiceCaller aSMLCaller = new ManageParticipantIdentifierServiceCaller (s_aSMLEndpointURL);

        switch (m_eAction)
        {
          case CREATE:
            // Undo create
            s_aLogger.warn ("CREATE failed in database, so deleting " +
                            m_aBusinessIdentifier.getURIEncoded () +
                            " from SML.");
            aSMLCaller.delete (m_aBusinessIdentifier);
            break;
          case DELETE:
            // Undo delete
            s_aLogger.warn ("DELETE failed in database, so creating " +
                            m_aBusinessIdentifier.getURIEncoded () +
                            " in SML.");
            aSMLCaller.create (s_sSMPID, m_aBusinessIdentifier);
            break;
        }
      }
      catch (final Throwable t)
      {
        throw new HookException ("Unable to rollback update business " + m_aBusinessIdentifier, t);
      }
  }
}
