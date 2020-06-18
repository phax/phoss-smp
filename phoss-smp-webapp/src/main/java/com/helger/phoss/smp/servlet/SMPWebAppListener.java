/**
 * Copyright (C) 2014-2020 Philip Helger and contributors
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
package com.helger.phoss.smp.servlet;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.commons.io.resource.IReadableResource;
import com.helger.commons.lang.priviledged.IPrivilegedAction;
import com.helger.commons.regex.RegExHelper;
import com.helger.commons.string.StringHelper;
import com.helger.network.proxy.ProxySelectorProxySettingsManager;
import com.helger.network.proxy.settings.IProxySettings;
import com.helger.network.proxy.settings.ProxySettingsManager;
import com.helger.pd.client.PDClientConfiguration;
import com.helger.pd.client.PDHttpClientSettings;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.app.PDClientProvider;
import com.helger.phoss.smp.app.SMPSecurity;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardCallback;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationCallback;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.phoss.smp.ui.ajax.CAjax;
import com.helger.phoss.smp.ui.pub.MenuPublic;
import com.helger.phoss.smp.ui.secure.MenuSecure;
import com.helger.photon.ajax.IAjaxRegistry;
import com.helger.photon.bootstrap4.pages.utils.BasePageUtilsHttpClient;
import com.helger.photon.bootstrap4.pages.utils.BasePageUtilsHttpClient.HttpClientConfig;
import com.helger.photon.bootstrap4.servlet.WebAppListenerBootstrap;
import com.helger.photon.core.appid.CApplicationID;
import com.helger.photon.core.appid.PhotonGlobalState;
import com.helger.photon.core.configfile.ConfigurationFile;
import com.helger.photon.core.configfile.ConfigurationFileManager;
import com.helger.photon.core.configfile.EConfigurationFileSyntax;
import com.helger.photon.core.locale.ILocaleManager;
import com.helger.photon.core.menu.MenuTree;
import com.helger.photon.core.requestparam.RequestParameterHandlerURLPathNamed;
import com.helger.photon.core.requestparam.RequestParameterManager;
import com.helger.servlet.ServletContextPathHolder;
import com.helger.servlet.response.UnifiedResponseDefaultSettings;
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
    return SMPWebAppConfiguration.getGlobalDebug ();
  }

  @Override
  protected String getInitParameterProduction (@Nonnull final ServletContext aSC)
  {
    return SMPWebAppConfiguration.getGlobalProduction ();
  }

  @Override
  protected String getDataPath (@Nonnull final ServletContext aSC)
  {
    return SMPWebAppConfiguration.getDataPath ();
  }

  @Override
  protected boolean shouldCheckFileAccess (@Nonnull final ServletContext aSC)
  {
    return SMPWebAppConfiguration.isCheckFileAccess ();
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
    if (SMPWebAppConfiguration.isGlobalDebugJaxWS ())
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

    // Handled via the XServletSettings instead
    UnifiedResponseDefaultSettings.setReferrerPolicy (null);
    UnifiedResponseDefaultSettings.setXFrameOptions (null, null);
    // SMP is http only
    UnifiedResponseDefaultSettings.removeStrictTransportSecurity ();

    // Check SMP ID
    final String sSMPID = SMPServerConfiguration.getSMLSMPID ();
    if (StringHelper.hasNoText (sSMPID))
      throw new IllegalArgumentException ("The SMP ID is missing. It must match the regular expression '" + CSMP.PATTERN_SMP_ID + "'!");
    if (!RegExHelper.stringMatchesPattern (CSMP.PATTERN_SMP_ID, sSMPID))
      throw new IllegalArgumentException ("The provided SMP ID '" +
                                          sSMPID +
                                          "' is not valid when used as a DNS name. It must match the regular expression '" +
                                          CSMP.PATTERN_SMP_ID +
                                          "'!");
    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("This SMP has the ID '" + sSMPID + "'");
  }

  @Override
  public void initLocales (@Nonnull final ILocaleManager aLocaleMgr)
  {
    aLocaleMgr.registerLocale (CSMPServer.DEFAULT_LOCALE);
    aLocaleMgr.setDefaultLocale (CSMPServer.DEFAULT_LOCALE);
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
  protected void initAjax (@Nonnull final IAjaxRegistry aAjaxRegistry)
  {
    CAjax.init (aAjaxRegistry);
  }

  @Override
  protected void initUI ()
  {
    // UI stuff
    SMPCommonUI.init ();
  }

  @Override
  protected void initSecurity ()
  {
    // Set all security related stuff
    SMPSecurity.init ();
  }

  @Override
  protected void initManagers ()
  {
    final ConfigurationFileManager aCFM = ConfigurationFileManager.getInstance ();
    aCFM.registerConfigurationFile (new ConfigurationFile (new ClassPathResource ("log4j2.xml")).setDescription ("Log4J2 configuration")
                                                                                                .setSyntaxHighlightLanguage (EConfigurationFileSyntax.XML));
    aCFM.registerConfigurationFile (new ConfigurationFile (SMPWebAppConfiguration.getSettingsResource ()).setDescription ("SMP web application configuration")
                                                                                                         .setSyntaxHighlightLanguage (EConfigurationFileSyntax.PROPERTIES));
    final IReadableResource aConfigRes = SMPServerConfiguration.getConfigFile ().getReadResource ();
    if (aConfigRes != null)
      aCFM.registerConfigurationFile (new ConfigurationFile (aConfigRes).setDescription ("SMP server configuration")
                                                                        .setSyntaxHighlightLanguage (EConfigurationFileSyntax.PROPERTIES));
    final IReadableResource aPDClientConfig = PDClientConfiguration.getConfigFile ().getReadResource ();
    if (aPDClientConfig != null)
      aCFM.registerConfigurationFile (new ConfigurationFile (aPDClientConfig).setDescription (SMPWebAppConfiguration.getDirectoryName () +
                                                                                              " client configuration")
                                                                             .setSyntaxHighlightLanguage (EConfigurationFileSyntax.PROPERTIES));

    // If the SMP settings change, the PD client must be re-created
    SMPMetaManager.getSettingsMgr ().callbacks ().add (x -> PDClientProvider.getInstance ().resetPDClient ());

    // Callback on BusinessCard manager - if something happens, notify PD server
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
    if (aBusinessCardMgr != null)
    {
      aBusinessCardMgr.bcCallbacks ().add (new ISMPBusinessCardCallback ()
      {
        public void onCreateOrUpdateSMPBusinessCard (@Nonnull final ISMPBusinessCard aBusinessCard)
        {
          final ISMPSettings aSettings = SMPMetaManager.getSettings ();
          if (aSettings.isDirectoryIntegrationEnabled () && aSettings.isDirectoryIntegrationAutoUpdate ())
          {
            // Notify PD server: add
            PDClientProvider.getInstance ()
                            .getPDClient ()
                            .addServiceGroupToIndex (aBusinessCard.getServiceGroup ().getParticpantIdentifier ());
          }
        }

        public void onDeleteSMPBusinessCard (@Nonnull final ISMPBusinessCard aBusinessCard)
        {
          final ISMPSettings aSettings = SMPMetaManager.getSettings ();
          if (aSettings.isDirectoryIntegrationEnabled () && aSettings.isDirectoryIntegrationAutoUpdate ())
          {
            // Notify PD server: delete
            PDClientProvider.getInstance ()
                            .getPDClient ()
                            .deleteServiceGroupFromIndex (aBusinessCard.getServiceGroup ().getParticpantIdentifier ());
          }
        }
      });

      // If a service information is create, updated or deleted, also update
      // Business Card at PD
      SMPMetaManager.getServiceInformationMgr ().serviceInformationCallbacks ().add (new ISMPServiceInformationCallback ()
      {
        public void onSMPServiceInformationCreated (@Nonnull final ISMPServiceInformation aServiceInformation)
        {
          final ISMPSettings aSettings = SMPMetaManager.getSettings ();
          if (aSettings.isDirectoryIntegrationEnabled () && aSettings.isDirectoryIntegrationAutoUpdate ())
          {
            // Only if a business card is present
            if (aBusinessCardMgr.containsSMPBusinessCardOfServiceGroup (aServiceInformation.getServiceGroup ()))
            {
              // Notify PD server: update
              PDClientProvider.getInstance ()
                              .getPDClient ()
                              .addServiceGroupToIndex (aServiceInformation.getServiceGroup ().getParticpantIdentifier ());
            }
          }
        }

        public void onSMPServiceInformationUpdated (@Nonnull final ISMPServiceInformation aServiceInformation)
        {
          final ISMPSettings aSettings = SMPMetaManager.getSettings ();
          if (aSettings.isDirectoryIntegrationEnabled () && aSettings.isDirectoryIntegrationAutoUpdate ())
          {
            // Only if a business card is present
            if (aBusinessCardMgr.containsSMPBusinessCardOfServiceGroup (aServiceInformation.getServiceGroup ()))
            {
              // Notify PD server: update
              PDClientProvider.getInstance ()
                              .getPDClient ()
                              .addServiceGroupToIndex (aServiceInformation.getServiceGroup ().getParticpantIdentifier ());
            }
          }
        }

        public void onSMPServiceInformationDeleted (@Nonnull final ISMPServiceInformation aServiceInformation)
        {
          final ISMPSettings aSettings = SMPMetaManager.getSettings ();
          if (aSettings.isDirectoryIntegrationEnabled () && aSettings.isDirectoryIntegrationAutoUpdate ())
          {
            // Only if a business card is present
            if (aBusinessCardMgr.containsSMPBusinessCardOfServiceGroup (aServiceInformation.getServiceGroup ()))
            {
              // Notify PD server: update
              PDClientProvider.getInstance ()
                              .getPDClient ()
                              .addServiceGroupToIndex (aServiceInformation.getServiceGroup ().getParticpantIdentifier ());
            }
          }
        }
      });
    }

    // Register global proxy servers
    ProxySelectorProxySettingsManager.setAsDefault (true);
    final IProxySettings aProxyHttp = SMPServerConfiguration.getAsHttpProxySettings ();
    if (aProxyHttp != null)
      ProxySettingsManager.registerProvider ( (sProtocol, sHost, nPort) -> "http".equals (sProtocol) ? new CommonsArrayList <> (aProxyHttp)
                                                                                                     : null);
    final IProxySettings aProxyHttps = SMPServerConfiguration.getAsHttpsProxySettings ();
    if (aProxyHttps != null)
      ProxySettingsManager.registerProvider ( (sProtocol,
                                               sHost,
                                               nPort) -> "https".equals (sProtocol) ? new CommonsArrayList <> (aProxyHttps) : null);

    // Special http client config
    BasePageUtilsHttpClient.HttpClientConfigRegistry.register (new HttpClientConfig ("directoryclient",
                                                                                     "Directory client settings",
                                                                                     x -> new PDHttpClientSettings (x)));
  }

  @Override
  protected void beforeContextDestroyed (@Nonnull final ServletContext aSC)
  {
    // Cleanup proxy selector (avoid ClassLoader leak)
    IPrivilegedAction.proxySelectorSetDefault (null);

    // Reset for unit tests
    BasePageUtilsHttpClient.HttpClientConfigRegistry.setToDefault ();
    super.beforeContextDestroyed (aSC);
  }
}
