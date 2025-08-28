/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
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

import java.net.ProxySelector;
import java.time.OffsetDateTime;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.helger.base.array.ArrayHelper;
import com.helger.base.debug.GlobalDebug;
import com.helger.base.exception.InitializationException;
import com.helger.base.string.StringHelper;
import com.helger.base.url.URLHelper;
import com.helger.cache.regex.RegExHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.zone.PDTConfig;
import com.helger.html.hc.config.HCSettings;
import com.helger.io.resource.ClassPathResource;
import com.helger.network.proxy.ProxySelectorProxySettingsManager;
import com.helger.network.proxy.settings.IProxySettings;
import com.helger.network.proxy.settings.IProxySettingsProvider;
import com.helger.network.proxy.settings.ProxySettingsManager;
import com.helger.pd.client.PDClient;
import com.helger.pd.client.PDClientConfiguration;
import com.helger.pd.client.PDHttpClientSettings;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.app.PDClientProvider;
import com.helger.phoss.smp.app.SMPSecurity;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.config.SMPConfigProvider;
import com.helger.phoss.smp.config.SMPHttpConfiguration;
import com.helger.phoss.smp.config.SMPServerConfiguration;
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
import com.helger.servlet.ServletSettings;
import com.helger.servlet.response.UnifiedResponseDefaultSettings;
import com.helger.smpclient.config.SMPClientConfiguration;
import com.helger.wsclient.WSHelper;
import com.helger.xservlet.requesttrack.RequestTrackerSettings;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.ServletContext;

/**
 * Special SMP web app listener. This is the entry point for application startup.
 *
 * @author Philip Helger
 */
public class SMPWebAppListener extends WebAppListenerBootstrap
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPWebAppListener.class);
  private static OffsetDateTime s_aStartupDateTime;

  private final ICommonsList <IProxySettingsProvider> m_aProxySettingsProvider = new CommonsArrayList <> ();

  @Nullable
  public static OffsetDateTime getStartupDateTime ()
  {
    return s_aStartupDateTime;
  }

  @Override
  @Nullable
  protected String getInitParameterDebug (@Nonnull final ServletContext aSC)
  {
    return SMPWebAppConfiguration.getGlobalDebug ();
  }

  @Override
  @Nullable
  protected String getInitParameterProduction (@Nonnull final ServletContext aSC)
  {
    return SMPWebAppConfiguration.getGlobalProduction ();
  }

  @Override
  @Nullable
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
  @Nullable
  protected String getInitParameterServerURL (@Nonnull final ServletContext aSC, final boolean bProductionMode)
  {
    // This is internally set in "StaticServerInfo" class
    final String sPublicURL = SMPServerConfiguration.getPublicServerURL ();
    if (StringHelper.isNotEmpty (sPublicURL))
    {
      // Check validity (see #237)
      if (URLHelper.getAsURL (sPublicURL, false) != null)
        return sPublicURL;

      final String sErrorMsg = "The configured public URL '" + sPublicURL + "' is not a valid URL!";
      LOGGER.error (sErrorMsg);
      throw new InitializationException (sErrorMsg);
    }
    return null;
  }

  private static void _initTimeZone ()
  {
    final String sDesiredTimeZone = SMPServerConfiguration.getTimeZoneOrDefault ();

    // Check if the timezone is supported
    if (!ArrayHelper.contains (TimeZone.getAvailableIDs (), sDesiredTimeZone))
    {
      final String sErrorMsg = "The default time zone '" + sDesiredTimeZone + "' is not supported!";
      LOGGER.error (sErrorMsg);
      throw new InitializationException (sErrorMsg);
    }
    // Set the default timezone
    if (PDTConfig.setDefaultDateTimeZoneID (sDesiredTimeZone).isFailure ())
    {
      final String sErrorMsg = "Failed to set default time zone to '" + sDesiredTimeZone + "'!";
      LOGGER.error (sErrorMsg);
      throw new InitializationException (sErrorMsg);
    }
    LOGGER.info ("Set default timezone to '" + sDesiredTimeZone + "'");
  }

  @Override
  protected void onTheVeryBeginning (final ServletContext aSC)
  {
    super.onTheVeryBeginning (aSC);

    _initTimeZone ();
  }

  @Override
  protected void initGlobalSettings ()
  {
    s_aStartupDateTime = PDTFactory.getCurrentOffsetDateTimeUTC ();

    // Enable JaxWS debugging?
    if (SMPWebAppConfiguration.isGlobalDebugJaxWS ())
      WSHelper.setMetroDebugSystemProperties (true);

    // JUL to SLF4J
    SLF4JBridgeHandler.removeHandlersForRootLogger ();
    SLF4JBridgeHandler.install ();

    // Set configs with variables
    PDClientConfiguration.setConfig (SMPConfigProvider.getConfig ());
    SMPClientConfiguration.setConfig (SMPConfigProvider.getConfig ());
    if (SMPServerConfiguration.isForceRoot ())
    {
      // Enforce an empty context path according to the specs!
      ServletContextPathHolder.setCustomContextPath ("");
    }
    RequestParameterManager.getInstance ().setParameterHandler (new RequestParameterHandlerURLPathNamed ());
    if (!GlobalDebug.isProductionMode ())
    {
      RequestTrackerSettings.setLongRunningRequestsCheckEnabled (false);
      RequestTrackerSettings.setParallelRunningRequestsCheckEnabled (false);
    }

    // Handled via the XServletSettings instead
    UnifiedResponseDefaultSettings.setReferrerPolicy (null);
    UnifiedResponseDefaultSettings.setXFrameOptions (null, null);
    if (SMPServerConfiguration.getRESTType ().isHttpAlsoAllowed ())
    {
      // Peppol SMP is always http only
      UnifiedResponseDefaultSettings.removeStrictTransportSecurity ();
    }

    // Make sure the nonce attributes are used
    // Required for CSP to work
    HCSettings.setUseNonceInScript (true);
    HCSettings.setUseNonceInStyle (true);

    // Don't add the session ID in the URL (since 7.2.3)
    ServletSettings.setEncodeURLs (false);

    // Avoid writing unnecessary stuff
    setHandleStatisticsOnEnd (SMPWebAppConfiguration.isPersistStatisticsOnEnd ());

    // Check SMP ID
    final String sSMPID = SMPServerConfiguration.getSMLSMPID ();
    if (StringHelper.isEmpty (sSMPID))
      throw new IllegalArgumentException ("The SMP ID is missing. It must match the regular expression '" +
                                          CSMPServer.PATTERN_SMP_ID +
                                          "'!");
    if (!RegExHelper.stringMatchesPattern (CSMPServer.PATTERN_SMP_ID, sSMPID))
      throw new IllegalArgumentException ("The provided SMP ID '" +
                                          sSMPID +
                                          "' is not valid when used as a DNS name. It must match the regular expression '" +
                                          CSMPServer.PATTERN_SMP_ID +
                                          "'!");
    LOGGER.info ("This SMP has the ID '" + sSMPID + "'");
    LOGGER.info ("This SMP uses REST API type '" + SMPServerConfiguration.getRESTType () + "'");

    // Check other consistency stuff
    if (SMPWebAppConfiguration.isImprintEnabled () && StringHelper.isEmpty (SMPWebAppConfiguration.getImprintText ()))
      LOGGER.warn ("The custom Imprint is enabled in the configuration, but no imprint text is configured. Therefore no imprint will be shown.");
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
    {
      LOGGER.info ("Init of ConfigurationFileManager");
      final ConfigurationFileManager aCFM = ConfigurationFileManager.getInstance ();
      aCFM.registerConfigurationFile (new ConfigurationFile (new ClassPathResource ("log4j2.xml")).setDescription ("Log4J2 configuration")
                                                                                                  .setSyntaxHighlightLanguage (EConfigurationFileSyntax.XML));
      aCFM.registerAll (SMPConfigProvider.getConfig ());
      aCFM.registerAll (PDClientConfiguration.getConfig ());
    }

    {
      LOGGER.info ("Init of Directory client stuff");

      // If the SMP settings change, the PD client must be re-created
      SMPMetaManager.getSettingsMgr ().callbacks ().add (x -> PDClientProvider.getInstance ().resetPDClient ());

      // Callback on BusinessCard manager - if something happens, notify PD
      // server
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
              PDClientProvider.getInstance ()
                              .getPDClient ()
                              .addServiceGroupToIndex (aBusinessCard.getParticipantIdentifier ());
            }
          }

          public void onSMPBusinessCardDeleted (@Nonnull final ISMPBusinessCard aBusinessCard)
          {
            final ISMPSettings aSettings = SMPMetaManager.getSettings ();
            if (aSettings.isDirectoryIntegrationEnabled () && aSettings.isDirectoryIntegrationAutoUpdate ())
            {
              // Notify PD server: delete
              final PDClient aPDClient = PDClientProvider.getInstance ().getPDClient ();

              // "Add" before "delete" to make sure it works
              // This will update the ownership in the Directory, in case a
              // certificate change happened since the last time
              aPDClient.addServiceGroupToIndex (aBusinessCard.getParticipantIdentifier ());

              // This is the actual delete call
              aPDClient.deleteServiceGroupFromIndex (aBusinessCard.getParticipantIdentifier ());
            }
          }
        });

        // If a service information is create, updated or deleted, also update
        // Business Card at PD
        SMPMetaManager.getServiceInformationMgr ()
                      .serviceInformationCallbacks ()
                      .add (new ISMPServiceInformationCallback ()
                      {
                        @Override
                        public void onSMPServiceInformationCreated (@Nonnull final ISMPServiceInformation aServiceInformation)
                        {
                          final ISMPSettings aSettings = SMPMetaManager.getSettings ();
                          if (aSettings.isDirectoryIntegrationEnabled () &&
                              aSettings.isDirectoryIntegrationAutoUpdate ())
                          {
                            // Only if a business card is present
                            final IParticipantIdentifier aPID = aServiceInformation.getServiceGroupParticipantIdentifier ();
                            if (aBusinessCardMgr.containsSMPBusinessCardOfID (aPID))
                            {
                              // Notify PD server: update
                              PDClientProvider.getInstance ().getPDClient ().addServiceGroupToIndex (aPID);
                            }
                          }
                        }

                        @Override
                        public void onSMPServiceInformationUpdated (@Nonnull final ISMPServiceInformation aServiceInformation)
                        {
                          onSMPServiceInformationCreated (aServiceInformation);
                        }

                        @Override
                        public void onSMPServiceInformationDeleted (@Nonnull final ISMPServiceInformation aServiceInformation)
                        {
                          onSMPServiceInformationCreated (aServiceInformation);
                        }
                      });
      }
    }

    final IProxySettings aProxyHttp = SMPHttpConfiguration.getAsHttpProxySettings ();
    final IProxySettings aProxyHttps = SMPHttpConfiguration.getAsHttpsProxySettings ();
    if (aProxyHttp != null || aProxyHttps != null)
    {
      LOGGER.info ("Init of HTTP and Proxy settings");

      // Register global proxy servers
      ProxySelectorProxySettingsManager.setAsDefault (true);
      if (aProxyHttp != null)
      {
        // Register a handler that returns the "http" proxy, if an "http" URL is
        // requested
        final IProxySettingsProvider aPSP = (sProtocol, sHost, nPort) -> "http".equals (sProtocol)
                                                                                                   ? new CommonsArrayList <> (aProxyHttp)
                                                                                                   : null;
        ProxySettingsManager.registerProvider (aPSP);
        m_aProxySettingsProvider.add (aPSP);
      }
      if (aProxyHttps != null)
      {
        // Register a handler that returns the "https" proxy, if an "https" URL
        // is requested
        final IProxySettingsProvider aPSP = (sProtocol, sHost, nPort) -> "https".equals (sProtocol)
                                                                                                    ? new CommonsArrayList <> (aProxyHttps)
                                                                                                    : null;
        ProxySettingsManager.registerProvider (aPSP);
        m_aProxySettingsProvider.add (aPSP);
      }
    }

    // Special http client config
    BasePageUtilsHttpClient.HttpClientConfigRegistry.register (new HttpClientConfig ("directoryclient",
                                                                                     "Directory client settings",
                                                                                     PDHttpClientSettings::new));

    LOGGER.info ("Finished init of managers");
  }

  @Override
  protected void beforeContextDestroyed (@Nonnull final ServletContext aSC)
  {
    // Explicitly unregister all proxy setting providers
    for (final IProxySettingsProvider aPSP : m_aProxySettingsProvider)
      ProxySettingsManager.unregisterProvider (aPSP);

    // Cleanup proxy selector (avoid ClassLoader leak)
    if (ProxySelectorProxySettingsManager.isDefault ())
      ProxySelector.setDefault (null);

    // Reset for unit tests
    BasePageUtilsHttpClient.HttpClientConfigRegistry.setToDefault ();
    super.beforeContextDestroyed (aSC);
  }
}
