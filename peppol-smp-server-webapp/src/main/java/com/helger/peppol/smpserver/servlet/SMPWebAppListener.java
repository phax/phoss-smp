/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.regex.RegExHelper;
import com.helger.pd.client.PDClientConfiguration;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.app.AppConfiguration;
import com.helger.peppol.smpserver.app.AppSecurity;
import com.helger.peppol.smpserver.app.CApp;
import com.helger.peppol.smpserver.app.PDClientProvider;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardCallback;
import com.helger.peppol.smpserver.ui.AppCommonUI;
import com.helger.peppol.smpserver.ui.ajax.CAjax;
import com.helger.peppol.smpserver.ui.pub.MenuPublic;
import com.helger.peppol.smpserver.ui.secure.MenuSecure;
import com.helger.photon.basic.app.appid.CApplicationID;
import com.helger.photon.basic.app.appid.PhotonGlobalState;
import com.helger.photon.basic.app.locale.ILocaleManager;
import com.helger.photon.basic.app.menu.MenuTree;
import com.helger.photon.basic.app.request.RequestParameterHandlerURLPathNamed;
import com.helger.photon.basic.app.request.RequestParameterManager;
import com.helger.photon.basic.configfile.ConfigurationFile;
import com.helger.photon.basic.configfile.ConfigurationFileManager;
import com.helger.photon.basic.configfile.EConfigurationFileSyntax;
import com.helger.photon.bootstrap3.servlet.WebAppListenerBootstrap;
import com.helger.photon.core.ajax.IAjaxInvoker;
import com.helger.servlet.ServletContextPathHolder;
import com.helger.wsclient.WSHelper;

/**
 * Special SMP web app listener. This is the entry point for application
 * startup.
 *
 * @author Philip Helger
 */
public class SMPWebAppListener extends WebAppListenerBootstrap
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPWebAppListener.class);

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
  protected void initGlobalSettings ()
  {
    // Enable JaxWS debugging?
    if (AppConfiguration.isGlobalDebugJaxWS ())
      WSHelper.setMetroDebugSystemProperties (true);

    // JUL to SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger ();
    SLF4JBridgeHandler.install ();

    if (SMPServerConfiguration.isForceRoot ())
    {
      // Enforce an empty context path according to the specs!
      ServletContextPathHolder.setCustomContextPath ("");
    }
    RequestParameterManager.getInstance ().setParameterHandler (new RequestParameterHandlerURLPathNamed ());

    // Check SMP ID
    final String sSMPID = SMPServerConfiguration.getSMLSMPID ();
    if (!RegExHelper.stringMatchesPattern (CApp.PATTERN_SMP_ID, sSMPID))
      throw new IllegalArgumentException ("The provided SMP ID '" +
                                          sSMPID +
                                          "' is not valid when used as a DNS name. It must match the regular expression '" +
                                          CApp.PATTERN_SMP_ID +
                                          "'!");
    LOGGER.info ("This SMP has the ID '" + sSMPID + "'");
  }

  @Override
  public void initLocales (@Nonnull final ILocaleManager aLocaleMgr)
  {
    aLocaleMgr.registerLocale (CApp.DEFAULT_LOCALE);
    aLocaleMgr.setDefaultLocale (CApp.DEFAULT_LOCALE);
  }

  @Override
  protected void initMenu ()
  {
    // Determine backend
    // Required before menu!
    SMPMetaManager.initBackendFromConfiguration ();

    // Create all menu items
    {
      final MenuTree aMenuTree = new MenuTree ();
      MenuPublic.init (aMenuTree);
      PhotonGlobalState.state (CApplicationID.APP_ID_PUBLIC).setMenuTree (aMenuTree);
    }
    {
      final MenuTree aMenuTree = new MenuTree ();
      MenuSecure.init (aMenuTree);
      PhotonGlobalState.state (CApplicationID.APP_ID_SECURE).setMenuTree (aMenuTree);
    }
  }

  @Override
  public void initAjax (@Nonnull final IAjaxInvoker aAjaxInvoker)
  {
    CAjax.init (aAjaxInvoker);
  }

  @Override
  protected void initUI ()
  {
    // UI stuff
    AppCommonUI.init ();
  }

  @Override
  protected void initSecurity ()
  {
    // Set all security related stuff
    AppSecurity.init ();
  }

  @Override
  protected void initManagers ()
  {
    final ConfigurationFileManager aCFM = ConfigurationFileManager.getInstance ();
    aCFM.registerConfigurationFile (new ConfigurationFile (new ClassPathResource ("log4j2.xml")).setDescription ("Log4J2 configuration")
                                                                                                .setSyntaxHighlightLanguage (EConfigurationFileSyntax.XML));
    aCFM.registerConfigurationFile (new ConfigurationFile (AppConfiguration.getSettingsResource ()).setDescription ("SMP web application configuration")
                                                                                                   .setSyntaxHighlightLanguage (EConfigurationFileSyntax.PROPERTIES));
    final IReadableResource aConfigRes = SMPServerConfiguration.getConfigFile ().getReadResource ();
    if (aConfigRes != null)
      aCFM.registerConfigurationFile (new ConfigurationFile (aConfigRes).setDescription ("SMP server configuration")
                                                                        .setSyntaxHighlightLanguage (EConfigurationFileSyntax.PROPERTIES));
    final IReadableResource aPDClientConfig = PDClientConfiguration.getConfigFile ().getReadResource ();
    if (aPDClientConfig != null)
      aCFM.registerConfigurationFile (new ConfigurationFile (aPDClientConfig).setDescription (AppConfiguration.getDirectoryName () +
                                                                                              " client configuration")
                                                                             .setSyntaxHighlightLanguage (EConfigurationFileSyntax.PROPERTIES));

    // If the SMP settings change, the PD client must be re-created
    SMPMetaManager.getSettingsMgr ().callbacks ().add (x -> PDClientProvider.getInstance ().resetPDClient ());

    // Callback on BusinessCard manager - if something happens, notify PD server
    SMPMetaManager.getBusinessCardMgr ().bcCallbacks ().add (new ISMPBusinessCardCallback ()
    {
      public void onCreateOrUpdateSMPBusinessCard (@Nonnull final ISMPBusinessCard aBusinessCard)
      {
        if (SMPMetaManager.getSettings ().isPEPPOLDirectoryIntegrationAutoUpdate ())
        {
          // Notify PD server: add
          PDClientProvider.getInstance ()
                          .getPDClient ()
                          .addServiceGroupToIndex (aBusinessCard.getServiceGroup ().getParticpantIdentifier ())
                          .isSuccess ();
        }
      }

      public void onDeleteSMPBusinessCard (@Nonnull final ISMPBusinessCard aBusinessCard)
      {
        if (SMPMetaManager.getSettings ().isPEPPOLDirectoryIntegrationAutoUpdate ())
        {
          // Notify PD server: delete
          PDClientProvider.getInstance ()
                          .getPDClient ()
                          .deleteServiceGroupFromIndex (aBusinessCard.getServiceGroup ().getParticpantIdentifier ())
                          .isSuccess ();
        }
      }
    });
  }
}
