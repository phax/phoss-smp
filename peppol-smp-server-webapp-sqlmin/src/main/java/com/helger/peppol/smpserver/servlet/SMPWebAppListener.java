/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.servlet;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import org.slf4j.bridge.SLF4JBridgeHandler;

import com.helger.commons.exception.InitializationException;
import com.helger.html.EHTMLVersion;
import com.helger.html.hc.config.HCSettings;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.app.AppSettings;
import com.helger.peppol.smpserver.backend.SMPBackendRegistry;
import com.helger.peppol.smpserver.domain.ISMPManagerProvider;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.photon.basic.app.request.ApplicationRequestManager;
import com.helger.photon.core.servlet.WebAppListener;
import com.helger.web.scope.mgr.WebScopeManager;

/**
 * Special SMP web app listener
 *
 * @author Philip Helger
 */
public class SMPWebAppListener extends WebAppListener
{
  @Override
  protected String getInitParameterDebug (@Nonnull final ServletContext aSC)
  {
    return AppSettings.getGlobalDebug ();
  }

  @Override
  protected String getInitParameterProduction (@Nonnull final ServletContext aSC)
  {
    return AppSettings.getGlobalProduction ();
  }

  @Override
  protected String getDataPath (@Nonnull final ServletContext aSC)
  {
    return AppSettings.getDataPath ();
  }

  @Override
  protected boolean shouldCheckFileAccess (@Nonnull final ServletContext aSC)
  {
    return AppSettings.isCheckFileAccess ();
  }

  @Override
  protected String getInitParameterServerURL (@Nonnull final ServletContext aSC, final boolean bProductionMode)
  {
    return SMPServerConfiguration.getPublicServerURL ();
  }

  public static void initBackendFromConfiguration ()
  {
    // Determine backend
    final String sBackendID = SMPServerConfiguration.getBackend ();
    final ISMPManagerProvider aManagerProvider = SMPBackendRegistry.getInstance ().getManagerProvider (sBackendID);
    if (aManagerProvider != null)
      SMPMetaManager.setManagerProvider (aManagerProvider);
    else
      throw new InitializationException ("Invalid backend '" +
                                         sBackendID +
                                         "' provided. Supported ones are: " +
                                         SMPBackendRegistry.getInstance ().getAllBackendIDs ());

    // Now we can call getInstance
    SMPMetaManager.getInstance ();
  }

  @Override
  protected void afterContextInitialized (@Nonnull final ServletContext aSC)
  {
    // JUL to SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger ();
    SLF4JBridgeHandler.install ();

    HCSettings.setDefaultHTMLVersion (EHTMLVersion.HTML5);

    // Internal stuff:
    if (SMPServerConfiguration.isForceRoot ())
    {
      // Enforce an empty context path according to the specs!
      WebScopeManager.getGlobalScope ().setCustomContextPath ("");
    }
    ApplicationRequestManager.getRequestMgr ().setUsePaths (true);

    // Determine backend
    initBackendFromConfiguration ();
  }
}
