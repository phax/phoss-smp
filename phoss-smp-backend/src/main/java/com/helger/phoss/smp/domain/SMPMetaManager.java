/**
 * Copyright (C) 2015-2020 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.exception.InitializationException;
import com.helger.commons.lang.ClassHelper;
import com.helger.commons.state.ETriState;
import com.helger.peppolid.factory.ESMPIdentifierType;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.backend.SMPBackendRegistry;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.LoggingSMPBusinessCardCallback;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.servicegroup.LoggingSMPServiceGroupCallback;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.phoss.smp.security.SMPTrustManager;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.settings.ISMPSettingsManager;
import com.helger.photon.core.mgr.PhotonBasicManager;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;
import com.helger.smpclient.url.ISMPURLProvider;
import com.helger.web.scope.mgr.WebScoped;

/**
 * The central SMP meta manager containing all the singleton manager instances.
 *
 * @author Philip Helger
 */
public final class SMPMetaManager extends AbstractGlobalSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMPMetaManager.class);

  private static ISMPManagerProvider s_aManagerProvider = null;

  private IIdentifierFactory m_aIdentifierFactory;
  private ISMPURLProvider m_aSMPURLProvider;
  private ISMLInfoManager m_aSMLInfoMgr;
  private ISMPSettingsManager m_aSettingsMgr;
  private ISMPTransportProfileManager m_aTransportProfileMgr;
  private ISMPServiceGroupManager m_aServiceGroupMgr;
  private ISMPRedirectManager m_aRedirectMgr;
  private ISMPServiceInformationManager m_aServiceInformationMgr;
  private ISMPBusinessCardManager m_aBusinessCardMgr;
  private ISMPParticipantMigrationManager m_aParticipantMigrationMgr;
  private ETriState m_eBackendConnectionEstablished = ETriState.UNDEFINED;

  /**
   * Set the manager provider to be used. This must be called exactly once
   * before {@link #getInstance()} is called.
   *
   * @param aManagerProvider
   *        The manager factory to be used. May be <code>null</code> for testing
   *        purposes.
   * @throws IllegalStateException
   *         If another manager provider is already present.
   */
  public static void setManagerProvider (@Nullable final ISMPManagerProvider aManagerProvider)
  {
    if (s_aManagerProvider != null && aManagerProvider != null)
      throw new IllegalStateException ("A manager provider is already set. You cannot set this twice! Call it with null before setting a new one");

    if (aManagerProvider != null && isGlobalSingletonInstantiated (SMPMetaManager.class))
      LOGGER.warn ("Setting the manager provider after singleton instantiation may not have the desired effect.");

    s_aManagerProvider = aManagerProvider;
    if (LOGGER.isInfoEnabled ())
    {
      if (aManagerProvider == null)
        LOGGER.info ("Using no backend manager provider");
      else
        LOGGER.info ("Using " + aManagerProvider + " as the backend manager provider");
    }
  }

  /**
   * @return The currently set manager provider. May be <code>null</code>.
   * @see #setManagerProvider(ISMPManagerProvider)
   */
  @Nullable
  public static ISMPManagerProvider getManagerProvider ()
  {
    return s_aManagerProvider;
  }

  @Deprecated
  @UsedViaReflection
  public SMPMetaManager ()
  {}

  private void _initCallbacks ()
  {
    // Always log
    m_aServiceGroupMgr.serviceGroupCallbacks ().add (new LoggingSMPServiceGroupCallback ());
    if (m_aBusinessCardMgr != null)
    {
      // If service group is deleted, also delete respective business card
      m_aServiceGroupMgr.serviceGroupCallbacks ().add (new BusinessCardSMPServiceGroupCallback (m_aBusinessCardMgr));
      m_aBusinessCardMgr.bcCallbacks ().add (new LoggingSMPBusinessCardCallback ());
    }
  }

  private void _performMigrations ()
  {
    // Required for SQL version
    try (final WebScoped aWS = new WebScoped ())
    {
      // See issue #128
      PhotonBasicManager.getSystemMigrationMgr ().performMigrationIfNecessary ("ensure-transport-profiles-128", () -> {
        LOGGER.info ("Started running migration to ensure all used transport profiles are automatically created");
        for (final ISMPServiceInformation aSI : m_aServiceInformationMgr.getAllSMPServiceInformation ())
          for (final ISMPProcess aProc : aSI.getAllProcesses ())
            for (final ISMPEndpoint aEP : aProc.getAllEndpoints ())
            {
              final String sTransportProfile = aEP.getTransportProfile ();
              if (!m_aTransportProfileMgr.containsSMPTransportProfileWithID (sTransportProfile))
              {
                m_aTransportProfileMgr.createSMPTransportProfile (sTransportProfile, sTransportProfile + " (automatically created)", false);
                LOGGER.info ("Created missing transport profile '" + sTransportProfile + "'");
              }
            }
      });
    }
  }

  @Override
  protected void onAfterInstantiation (@Nonnull final IScope aScope)
  {
    if (s_aManagerProvider == null)
      throw new InitializationException ("No ManagerProvider is set. Please call setManagerProvider before you call getInstance!");

    try
    {
      // Before all
      s_aManagerProvider.beforeInitManagers ();

      final ESMPIdentifierType eIdentifierType = SMPServerConfiguration.getIdentifierType ();
      m_aIdentifierFactory = eIdentifierType.getIdentifierFactory ();

      // Initialize first because the service group manager initializes the
      // RegistrationHookFactory
      try
      {
        SMPTrustManager.getInstance ();
      }
      catch (final Exception ex)
      {
        // fall through. No special trust store - no problem :)
      }
      try
      {
        SMPKeyManager.getInstance ();
      }
      catch (final Exception ex)
      {
        // fall through. Certificate stays invalid, no SML access possible.
      }

      m_eBackendConnectionEstablished = s_aManagerProvider.getBackendConnectionEstablishedDefaultState ();
      if (m_eBackendConnectionEstablished == null)
        throw new IllegalStateException ("Failed to get default backend connection state!");

      m_aSMPURLProvider = s_aManagerProvider.createSMPURLProvider ();
      if (m_aSMPURLProvider == null)
        throw new IllegalStateException ("Failed to create SMP URL Provider!");

      m_aSMLInfoMgr = s_aManagerProvider.createSMLInfoMgr ();
      if (m_aSMLInfoMgr == null)
        throw new IllegalStateException ("Failed to create SML Info manager!");

      m_aSettingsMgr = s_aManagerProvider.createSettingsMgr ();
      if (m_aSettingsMgr == null)
        throw new IllegalStateException ("Failed to create Settings manager!");

      m_aTransportProfileMgr = s_aManagerProvider.createTransportProfileMgr ();
      if (m_aTransportProfileMgr == null)
        throw new IllegalStateException ("Failed to create TransportProfile manager!");

      // Service group manager must be before redirect and service information!
      m_aServiceGroupMgr = s_aManagerProvider.createServiceGroupMgr ();
      if (m_aServiceGroupMgr == null)
        throw new IllegalStateException ("Failed to create ServiceGroup manager!");

      m_aRedirectMgr = s_aManagerProvider.createRedirectMgr (m_aIdentifierFactory, m_aServiceGroupMgr);
      if (m_aRedirectMgr == null)
        throw new IllegalStateException ("Failed to create Redirect manager!");

      m_aServiceInformationMgr = s_aManagerProvider.createServiceInformationMgr (m_aIdentifierFactory, m_aServiceGroupMgr);
      if (m_aServiceInformationMgr == null)
        throw new IllegalStateException ("Failed to create ServiceInformation manager!");

      m_aParticipantMigrationMgr = s_aManagerProvider.createParticipantMigrationMgr ();
      if (m_aParticipantMigrationMgr == null)
        throw new IllegalStateException ("Failed to create ParticipantMigration manager!");

      // May be null!
      m_aBusinessCardMgr = s_aManagerProvider.createBusinessCardMgr (m_aIdentifierFactory, m_aServiceGroupMgr);

      _initCallbacks ();

      _performMigrations ();

      // After all
      s_aManagerProvider.afterInitManagers ();

      LOGGER.info (ClassHelper.getClassLocalName (this) + " was initialized");
    }
    catch (final Exception ex)
    {
      throw new InitializationException ("Failed to init " + ClassHelper.getClassLocalName (this), ex);
    }
  }

  @Nonnull
  public static SMPMetaManager getInstance ()
  {
    return getGlobalSingleton (SMPMetaManager.class);
  }

  @Nonnull
  public static IIdentifierFactory getIdentifierFactory ()
  {
    return getInstance ().m_aIdentifierFactory;
  }

  @Nonnull
  public static ISMPURLProvider getSMPURLProvider ()
  {
    return getInstance ().m_aSMPURLProvider;
  }

  @Nonnull
  public static ISMLInfoManager getSMLInfoMgr ()
  {
    return getInstance ().m_aSMLInfoMgr;
  }

  @Nonnull
  public static ISMPSettingsManager getSettingsMgr ()
  {
    return getInstance ().m_aSettingsMgr;
  }

  @Nonnull
  public static ISMPSettings getSettings ()
  {
    return getSettingsMgr ().getSettings ();
  }

  @Nonnull
  public static ISMPTransportProfileManager getTransportProfileMgr ()
  {
    return getInstance ().m_aTransportProfileMgr;
  }

  @Nonnull
  public static ISMPServiceGroupManager getServiceGroupMgr ()
  {
    return getInstance ().m_aServiceGroupMgr;
  }

  @Nonnull
  public static ISMPRedirectManager getRedirectMgr ()
  {
    return getInstance ().m_aRedirectMgr;
  }

  @Nonnull
  public static ISMPServiceInformationManager getServiceInformationMgr ()
  {
    return getInstance ().m_aServiceInformationMgr;
  }

  @Nonnull
  public static ISMPParticipantMigrationManager getParticipantMigrationMgr ()
  {
    return getInstance ().m_aParticipantMigrationMgr;
  }

  @Nullable
  public static ISMPBusinessCardManager getBusinessCardMgr ()
  {
    return getInstance ().m_aBusinessCardMgr;
  }

  public static boolean hasBusinessCardMgr ()
  {
    return getBusinessCardMgr () != null;
  }

  @Nonnull
  public ETriState getBackendConnectionEstablished ()
  {
    return m_aRWLock.readLockedGet ( () -> m_eBackendConnectionEstablished);
  }

  public void setBackendConnectionEstablished (@Nonnull final ETriState eConnectionEstablished)
  {
    ValueEnforcer.notNull (eConnectionEstablished, "ConnectionEstablished");
    m_aRWLock.writeLockedGet ( () -> m_eBackendConnectionEstablished = eConnectionEstablished);
  }

  /**
   * This is the initialization routine that must be called upon application
   * startup. It performs the SPI initialization of all registered manager
   * provider ({@link ISMPManagerProvider}) and selects the one specified in the
   * SMP server configuration file.
   *
   * @throws InitializationException
   *         If an unsupported backend is provided in the configuration.
   * @see SMPServerConfiguration#getBackend()
   * @see SMPBackendRegistry
   * @see ISMPManagerProvider
   * @see #setManagerProvider(ISMPManagerProvider)
   */
  public static void initBackendFromConfiguration ()
  {
    // Determine backend
    final SMPBackendRegistry aBackendRegistry = SMPBackendRegistry.getInstance ();
    final String sBackendID = SMPServerConfiguration.getBackend ();
    final ISMPManagerProvider aManagerProvider = aBackendRegistry.getManagerProvider (sBackendID);
    if (aManagerProvider == null)
      throw new InitializationException ("Invalid backend '" +
                                         sBackendID +
                                         "' provided. Supported ones are: " +
                                         aBackendRegistry.getAllBackendIDs ());

    // Remember the manager provider
    setManagerProvider (aManagerProvider);

    // Now we can call getInstance to ensure everything is initialized correctly
    getInstance ();
  }
}
