/**
 * Copyright (C) 2014-2017 Philip Helger (www.helger.com)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.helger.commons.collection.ext.CommonsHashMap;
import com.helger.commons.collection.ext.ICommonsMap;
import com.helger.commons.regex.RegExHelper;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.app.AppConfiguration;
import com.helger.peppol.smpserver.app.AppSecurity;
import com.helger.peppol.smpserver.app.CApp;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.ui.AppCommonUI;
import com.helger.peppol.smpserver.ui.pub.InitializerPublic;
import com.helger.peppol.smpserver.ui.secure.InitializerSecure;
import com.helger.photon.basic.app.request.RequestParameterHandlerURLPathNamed;
import com.helger.photon.basic.app.request.RequestParameterManager;
import com.helger.photon.core.app.CApplication;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.photon.core.app.init.IApplicationInitializer;
import com.helger.photon.core.servlet.AbstractWebAppListenerMultiApp;
import com.helger.servlet.ServletContextPathHolder;

/**
 * Special SMP web app listener. This is the entry point for application
 * startup.
 *
 * @author Philip Helger
 */
public class SMPWebAppListener extends AbstractWebAppListenerMultiApp <LayoutExecutionContext>
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (SMPWebAppListener.class);

  @Override
  protected String getInitParameterDebug (@Nonnull final ServletContext aSC)
  {
    return AppConfiguration.getGlobalDebug ();
  }

  @Override
  protected String getInitParameterProduction (@Nonnull final ServletContext aSC)
  {
    return AppConfiguration.getGlobalProduction ();
  }

  @Override
  protected String getDataPath (@Nonnull final ServletContext aSC)
  {
    return AppConfiguration.getDataPath ();
  }

  @Override
  protected boolean shouldCheckFileAccess (@Nonnull final ServletContext aSC)
  {
    return AppConfiguration.isCheckFileAccess ();
  }

  @Override
  protected String getInitParameterServerURL (@Nonnull final ServletContext aSC, final boolean bProductionMode)
  {
    return SMPServerConfiguration.getPublicServerURL ();
  }

  @Override
  protected void initGlobals ()
  {
    // Internal stuff:

    // JUL to SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger ();
    SLF4JBridgeHandler.install ();

    super.initGlobals ();

    if (SMPServerConfiguration.isForceRoot ())
    {
      // Enforce an empty context path according to the specs!
      ServletContextPathHolder.setCustomContextPath ("");
    }
    RequestParameterManager.getInstance ().setParameterHandler (new RequestParameterHandlerURLPathNamed ());

    // UI stuff
    AppCommonUI.init ();

    // Set all security related stuff
    AppSecurity.init ();

    // Determine backend
    SMPMetaManager.initBackendFromConfiguration ();

    // Check SMP ID
    final String sSMPID = SMPServerConfiguration.getSMLSMPID ();
    if (!RegExHelper.stringMatchesPattern (CApp.PATTERN_SMP_ID, sSMPID))
      throw new IllegalArgumentException ("The provided SMP ID '" +
                                          sSMPID +
                                          "' is not valid when used as a DNS name. It must match the regular expression '" +
                                          CApp.PATTERN_SMP_ID +
                                          "'!");
    s_aLogger.info ("This SMP has the ID '" + sSMPID + "'");
  }

  @Override
  @Nonnull
  protected ICommonsMap <String, IApplicationInitializer <LayoutExecutionContext>> getAllInitializers ()
  {
    final ICommonsMap <String, IApplicationInitializer <LayoutExecutionContext>> ret = new CommonsHashMap<> ();
    ret.put (CApplication.APP_ID_PUBLIC, new InitializerPublic ());
    ret.put (CApplication.APP_ID_SECURE, new InitializerSecure ());
    return ret;
  }
}
