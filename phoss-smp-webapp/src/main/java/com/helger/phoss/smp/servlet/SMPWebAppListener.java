/**
 * Copyright (C) 2014-2021 Philip Helger and contributors
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

import java.time.OffsetDateTime;
import java.util.TimeZone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.datetime.PDTConfig;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.exception.InitializationException;
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
import com.helger.xservlet.requesttrack.RequestTrackerSettings;

/**
 * Special SMP web app listener. This is the entry point for application
 * startup.
 *
 * @author Philip Helger
 */
public class SMPWebAppListener extends WebAppListenerBootstrap
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPWebAppListener.class);
  private static OffsetDateTime s_aStartupDateTime;

  @Nullable
  public static OffsetDateTime getStartupDateTime ()
  {
    return s_aStartupDateTime;
  }

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
    // This is internally set in "StaticServerInfo" class
    return SMPServerConfiguration.getPublicServerURL ();
  }

  @Override
  protected void initGlobalSettings ()
  {
    // Check if the timezone is supported
    if (!ArrayHelper.contains (TimeZone.getAvailableIDs (), CSMP.DEFAULT_TIMEZONE))
    {
      final String sErrorMsg = "The default time zone '" + CSMP.DEFAULT_TIMEZONE + "' is not supported!";
      LOGGER.error (sErrorMsg);
      throw new InitializationException (sErrorMsg);
    }

    // Set the default timezone
    if (PDTConfig.setDefaultDateTimeZoneID (CSMP.DEFAULT_TIMEZONE).isFailure ())
    {
      final String sErrorMsg = "Failed to set default time zone to '" + CSMP.DEFAULT_TIMEZONE + "'!";
      LOGGER.error (sErrorMsg);
      throw new InitializationException (sErrorMsg);
    }
    LOGGER.info ("Set default timezone to '" + CSMP.DEFAULT_TIMEZONE + "'");

    s_aStartupDateTime = PDTFactory.getCurrentOffsetDateTime ();

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

    if (GlobalDebug.isDebugMode ())
    {
      RequestTrackerSettings.setLongRunningRequestsCheckEnabled (false);
      RequestTrackerSettings.setParallelRunningRequestsCheckEnabled (false);
    }

    // Handled via the XServletSettings instead
    UnifiedResponseDefaultSettings.setReferrerPolicy (null);
    UnifiedResponseDefaultSettings.setXFrameOptions (null, null);
    // SMP is http only
    UnifiedResponseDefaultSettings.removeStrictTransportSecurity ();

    // Avoid writing unnecessary stuff
    setHandleStatisticsOnEnd (SMPWebAppConfiguration.isPersistStatisticsOnEnd ());

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

    if (SMPWebAppConfiguration.isImprintEnabled () && StringHelper.hasNoText (SMPWebAppConfiguration.getImprintText ()))
      LOGGER.warn ("The custom Imprint is enabled in the configuration file, but no imprint text is configured. Therefore no imprint will be shown.");

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
      LOGGER.info ("Initializing public menu");
      final MenuTree aMenuTree = new MenuTree ();
      MenuPublic.init (aMenuTree);
      PhotonGlobalState.state (CApplicationID.APP_ID_PUBLIC).setMenuTree (aMenuTree);
    }
    {
      LOGGER.info ("Initializing secure menu");
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
  protected void initSecurity ()
  {
    // Set all security related stuff
    SMPSecurity.init ();
  }

  @Override
  protected void initUI ()
  {
    // UI stuff
    SMPCommonUI.init ();
  }

  @Override
  protected void initManagers ()
  {
    LOGGER.info ("Init of ConfigurationFileManager");
    final ConfigurationFileManager aCFM = ConfigurationFileManager.getInstance ();
    aCFM.registerConfigurationFile (new ConfigurationFile (new ClassPathResource ("log4j2.xml")).setDescription ("Log4J2 configuration")
                                                                                                .setSyntaxHighlightLanguage (EConfigurationFileSyntax.XML));
    aCFM.registerConfigurationFile (new ConfigurationFile (SMPWebAppConfiguration.getSettingsResource ()).setDescription ("SMP web application configuration")
                                                                                                         .setSyntaxHighlightLanguage (EConfigurationFileSyntax.PROPERTIES));
    final IReadableResource aConfigRes = SMPServerConfiguration.getConfigFile ().getReadResource ();
    if (aConfigRes != null)
      aCFM.registerConfigurationFile (new ConfigurationFile (aConfigRes).setDescription ("SMP server configuration")
                                                                        .setSyntaxHighlightLanguage (EConfigurationFileSyntax.PROPERTIES));
    aCFM.registerAll (PDClientConfiguration.getConfig ());

    // If the SMP settings change, the PD client must be re-created
    SMPMetaManager.getSettingsMgr ().callbacks ().add (x -> PDClientProvider.getInstance ().resetPDClient ());

    // Callback on BusinessCard manager - if something happens, notify PD server
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
    if (aBusinessCardMgr != null)
    {
      aBusinessCardMgr.bcCallbacks ().add (new ISMPBusinessCardCallback ()
      {
        public void onSMPBusinessCardCreatedOrUpdated (@Nonnull final ISMPBusinessCard aBusinessCard)
        {
          final ISMPSettings aSettings = SMPMetaManager.getSettings ();
          if (aSettings.isDirectoryIntegrationEnabled () && aSettings.isDirectoryIntegrationAutoUpdate ())
          {
            // Notify PD server: add
            PDClientProvider.getInstance ().getPDClient ().addServiceGroupToIndex (aBusinessCard.getParticipantIdentifier ());
          }
        }

        public void onSMPBusinessCardDeleted (@Nonnull final ISMPBusinessCard aBusinessCard)
        {
          final ISMPSettings aSettings = SMPMetaManager.getSettings ();
          if (aSettings.isDirectoryIntegrationEnabled () && aSettings.isDirectoryIntegrationAutoUpdate ())
          {
            // Notify PD server: delete
            PDClientProvider.getInstance ().getPDClient ().deleteServiceGroupFromIndex (aBusinessCard.getParticipantIdentifier ());
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
          onSMPServiceInformationCreated (aServiceInformation);
        }

        public void onSMPServiceInformationDeleted (@Nonnull final ISMPServiceInformation aServiceInformation)
        {
          onSMPServiceInformationCreated (aServiceInformation);
        }
      });
    }

    LOGGER.info ("Init of HTTP and Proxy settings");
    // Register global proxy servers
    ProxySelectorProxySettingsManager.setAsDefault (true);
    final IProxySettings aProxyHttp = SMPServerConfiguration.getAsHttpProxySettings ();
    if (aProxyHttp != null)
    {
      // Register a handler that returns the "http" proxy, if an "http" URL is
      // requested
      ProxySettingsManager.registerProvider ( (sProtocol, sHost, nPort) -> "http".equals (sProtocol) ? new CommonsArrayList <> (aProxyHttp)
                                                                                                     : null);
    }
    final IProxySettings aProxyHttps = SMPServerConfiguration.getAsHttpsProxySettings ();
    if (aProxyHttps != null)
    {
      // Register a handler that returns the "https" proxy, if an "https" URL is
      // requested
      ProxySettingsManager.registerProvider ( (sProtocol,
                                               sHost,
                                               nPort) -> "https".equals (sProtocol) ? new CommonsArrayList <> (aProxyHttps) : null);
    }

    // Special http client config
    BasePageUtilsHttpClient.HttpClientConfigRegistry.register (new HttpClientConfig ("directoryclient",
                                                                                     "Directory client settings",
                                                                                     PDHttpClientSettings::new));
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
