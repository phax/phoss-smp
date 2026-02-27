/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain;

import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.exception.InitializationException;
import com.helger.base.lang.clazz.ClassHelper;
import com.helger.base.state.ETriState;
import com.helger.peppolid.factory.ESMPIdentifierType;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.backend.SMPBackendRegistry;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.LoggingSMPBusinessCardCallback;
import com.helger.phoss.smp.domain.spf.ISMPSPF4PeppolPolicyManager;
import com.helger.phoss.smp.domain.spf.LoggingSMPSPF4PeppolCallback;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.redirect.LoggingSMPRedirectCallback;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.servicegroup.LoggingSMPServiceGroupCallback;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.LoggingSMPServiceInformationCallback;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.phoss.smp.security.SMPTrustManager;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.settings.ISMPSettingsManager;
import com.helger.photon.mgrs.PhotonBasicManager;
import com.helger.scope.IScope;
import com.helger.scope.singleton.AbstractGlobalSingleton;
import com.helger.smpclient.url.BDXLURLProvider;
import com.helger.smpclient.url.ISMPURLProvider;
import com.helger.smpclient.url.PeppolNaptrURLProvider;
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
  private ISMPSPF4PeppolPolicyManager m_aSPFPolicyMgr;
  private ISMPParticipantMigrationManager m_aParticipantMigrationMgr;
  private ETriState m_eBackendConnectionState = ETriState.UNDEFINED;
  private Consumer <ETriState> m_aBackendConnectionStateChangeCallback;

  /**
   * Set the manager provider to be used. This must be called exactly once before
   * {@link #getInstance()} is called.
   *
   * @param aManagerProvider
   *        The manager factory to be used. May be <code>null</code> for testing purposes.
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
    if (aManagerProvider == null)
      LOGGER.info ("Using no backend manager provider");
    else
      LOGGER.info ("Using " + aManagerProvider + " as the backend manager provider");
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

  /**
   * @deprecated Only called via reflection
   */
  @Deprecated (forRemoval = false)
  @UsedViaReflection
  public SMPMetaManager ()
  {}

  private void _initCallbacks ()
  {
    // Always log
    m_aServiceGroupMgr.serviceGroupCallbacks ().add (new LoggingSMPServiceGroupCallback ());
    m_aRedirectMgr.redirectCallbacks ().add (new LoggingSMPRedirectCallback ());
    m_aServiceInformationMgr.serviceInformationCallbacks ().add (new LoggingSMPServiceInformationCallback ());

    if (m_aBusinessCardMgr != null)
    {
      // If service group is deleted, also delete respective business card
      m_aServiceGroupMgr.serviceGroupCallbacks ().add (new BusinessCardSMPServiceGroupCallback (m_aBusinessCardMgr));
      if (false)
      {
        // #198; this causes a problem when finalizing an outbound migration -
        // because it deletes the migration and prevents it from being shown
        m_aServiceGroupMgr.serviceGroupCallbacks ()
                          .add (new ParticipantMigrationSMPServiceGroupCallback (m_aParticipantMigrationMgr));
      }

      // Always log
      m_aBusinessCardMgr.bcCallbacks ().add (new LoggingSMPBusinessCardCallback ());
    }

    if (m_aSPFPolicyMgr != null)
    {
      // Always log
      m_aSPFPolicyMgr.spfCallbacks ().add (new LoggingSMPSPF4PeppolCallback ());
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
                m_aTransportProfileMgr.createSMPTransportProfile (sTransportProfile,
                                                                  sTransportProfile + " (automatically created)",
                                                                  false);
                LOGGER.info ("Created missing transport profile '" + sTransportProfile + "'");
              }
            }
      });
    }
  }

  @Override
  protected void onAfterInstantiation (@NonNull final IScope aScope)
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

      m_eBackendConnectionState = s_aManagerProvider.getBackendConnectionEstablishedDefaultState ();
      if (m_eBackendConnectionState == null)
        throw new IllegalStateException ("Failed to get default backend connection state!");

      // TODO This might become configurable in the future
      m_aSMPURLProvider = SMPServerConfiguration.getRESTType ().isPeppol () ? PeppolNaptrURLProvider.INSTANCE
                                                                            : BDXLURLProvider.INSTANCE;
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

      m_aRedirectMgr = s_aManagerProvider.createRedirectMgr (m_aIdentifierFactory);
      if (m_aRedirectMgr == null)
        throw new IllegalStateException ("Failed to create Redirect manager!");

      m_aServiceInformationMgr = s_aManagerProvider.createServiceInformationMgr (m_aIdentifierFactory);
      if (m_aServiceInformationMgr == null)
        throw new IllegalStateException ("Failed to create ServiceInformation manager!");

      m_aParticipantMigrationMgr = s_aManagerProvider.createParticipantMigrationMgr ();
      if (m_aParticipantMigrationMgr == null)
        throw new IllegalStateException ("Failed to create ParticipantMigration manager!");

      // May be null!
      m_aBusinessCardMgr = s_aManagerProvider.createBusinessCardMgr (m_aIdentifierFactory, m_aServiceGroupMgr);

      // May be null!
      m_aSPFPolicyMgr = s_aManagerProvider.createSPFPolicyMgr (m_aIdentifierFactory, m_aServiceGroupMgr);

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

  @NonNull
  public static SMPMetaManager getInstance ()
  {
    return getGlobalSingleton (SMPMetaManager.class);
  }

  @NonNull
  public static IIdentifierFactory getIdentifierFactory ()
  {
    return getInstance ().m_aIdentifierFactory;
  }

  @NonNull
  public static ISMPURLProvider getSMPURLProvider ()
  {
    return getInstance ().m_aSMPURLProvider;
  }

  @NonNull
  public static ISMLInfoManager getSMLInfoMgr ()
  {
    return getInstance ().m_aSMLInfoMgr;
  }

  @NonNull
  public static ISMPSettingsManager getSettingsMgr ()
  {
    return getInstance ().m_aSettingsMgr;
  }

  @NonNull
  public static ISMPSettings getSettings ()
  {
    return getSettingsMgr ().getSettings ();
  }

  @NonNull
  public static ISMPTransportProfileManager getTransportProfileMgr ()
  {
    return getInstance ().m_aTransportProfileMgr;
  }

  @NonNull
  public static ISMPServiceGroupManager getServiceGroupMgr ()
  {
    return getInstance ().m_aServiceGroupMgr;
  }

  @NonNull
  public static ISMPRedirectManager getRedirectMgr ()
  {
    return getInstance ().m_aRedirectMgr;
  }

  @NonNull
  public static ISMPServiceInformationManager getServiceInformationMgr ()
  {
    return getInstance ().m_aServiceInformationMgr;
  }

  @NonNull
  public static ISMPParticipantMigrationManager getParticipantMigrationMgr ()
  {
    return getInstance ().m_aParticipantMigrationMgr;
  }

  @Nullable
  public static ISMPBusinessCardManager getBusinessCardMgr ()
  {
    return getInstance ().m_aBusinessCardMgr;
  }

  /**
   * @return <code>true</code> if an {@link ISMPBusinessCardManager} is present, <code>false</code>
   *         if not.
   */
  public static boolean hasBusinessCardMgr ()
  {
    return getBusinessCardMgr () != null;
  }

  @Nullable
  public static ISMPSPF4PeppolPolicyManager getSPFPolicyMgr ()
  {
    return getInstance ().m_aSPFPolicyMgr;
  }

  /**
   * @return <code>true</code> if an {@link ISMPSPF4PeppolPolicyManager} is present,
   *         <code>false</code> if not.
   * @since 8.0.13
   */
  public static boolean hasSPFPolicyMgr ()
  {
    return getSPFPolicyMgr () != null;
  }

  @NonNull
  public ETriState getBackendConnectionState ()
  {
    return m_aRWLock.readLockedGet ( () -> m_eBackendConnectionState);
  }

  public void setBackendConnectionState (@NonNull final ETriState eConnectionEstablished,
                                         final boolean bTriggerCallback)
  {
    ValueEnforcer.notNull (eConnectionEstablished, "ConnectionEstablished");

    m_aRWLock.writeLocked ( () -> {
      m_eBackendConnectionState = eConnectionEstablished;

      // Avoid endless loop
      if (bTriggerCallback && m_aBackendConnectionStateChangeCallback != null)
        m_aBackendConnectionStateChangeCallback.accept (eConnectionEstablished);
    });
  }

  /**
   * Set the SMP callback that should be invoked if the backend connection established state
   * changed. This needs to be a custom callback for dependency reasons and is only used by the SQL
   * backend.
   *
   * @param aCB
   *        The callback to invoke. May be <code>null</code>.
   */
  public void setBackendConnectionStateChangeCallback (@Nullable final Consumer <ETriState> aCB)
  {
    m_aRWLock.writeLocked ( () -> m_aBackendConnectionStateChangeCallback = aCB);
  }

  /**
   * This is the initialization routine that must be called upon application startup. It performs
   * the SPI initialization of all registered manager provider ({@link ISMPManagerProvider}) and
   * selects the one specified in the SMP server configuration file.
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

    LOGGER.info ("Initializing SMP manager for backend '" + sBackendID + "'");

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
