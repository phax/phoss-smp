/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import org.slf4j.bridge.SLF4JBridgeHandler;

import com.helger.commons.exception.InitializationException;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.app.AppSecurity;
import com.helger.peppol.smpserver.app.AppSettings;
import com.helger.peppol.smpserver.backend.SMPBackendRegistry;
import com.helger.peppol.smpserver.domain.ISMPManagerProvider;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.ui.AppCommonUI;
import com.helger.peppol.smpserver.ui.pub.InitializerPublic;
import com.helger.peppol.smpserver.ui.secure.InitializerSecure;
import com.helger.photon.basic.app.request.ApplicationRequestManager;
import com.helger.photon.core.app.CApplication;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.photon.core.app.init.IApplicationInitializer;
import com.helger.photon.core.servlet.AbstractWebAppListenerMultiApp;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.security.role.RoleManager;
import com.helger.photon.security.user.UserManager;
import com.helger.photon.security.usergroup.UserGroupManager;
import com.helger.web.scope.mgr.WebScopeManager;

/**
 * Special SMP web app listener
 *
 * @author Philip Helger
 */
public class SMPWebAppListener extends AbstractWebAppListenerMultiApp <LayoutExecutionContext>
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

  @Override
  @Nonnull
  protected Map <String, IApplicationInitializer <LayoutExecutionContext>> getAllInitializers ()
  {
    final Map <String, IApplicationInitializer <LayoutExecutionContext>> ret = new HashMap <String, IApplicationInitializer <LayoutExecutionContext>> ();
    ret.put (CApplication.APP_ID_PUBLIC, new InitializerPublic ());
    ret.put (CApplication.APP_ID_SECURE, new InitializerSecure ());
    return ret;
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
  protected void initGlobals ()
  {
    // Internal stuff:

    // JUL to SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger ();
    SLF4JBridgeHandler.install ();

    super.initGlobals ();

    // Call before accessing PhotonSecurityManager!
    RoleManager.setCreateDefaults (false);
    UserManager.setCreateDefaults (false);
    UserGroupManager.setCreateDefaults (false);

    if (SMPServerConfiguration.isForceRoot ())
    {
      // Enforce an empty context path according to the specs!
      WebScopeManager.getGlobalScope ().setCustomContextPath ("");
    }
    ApplicationRequestManager.getRequestMgr ().setUsePaths (true);

    // UI stuff
    AppCommonUI.init ();

    // Set all security related stuff
    AppSecurity.init ();

    // New login logs out old user
    LoggedInUserManager.getInstance ().setLogoutAlreadyLoggedInUser (true);

    // Determine backend
    initBackendFromConfiguration ();
  }
}
