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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.state.ETriState;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.spf.ISMPSPF4PeppolPolicyManager;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.settings.ISMPSettingsManager;

/**
 * An abstract manager provider interface. This must be implemented for each
 * supported backend. The correct implementation must be set in the MetaManager
 * before instantiating it.
 *
 * @author Philip Helger
 */
public interface ISMPManagerProvider
{
  /**
   * Callback to perform actions BEFORE the managers are initialized.
   *
   * @see #afterInitManagers()
   */
  default void beforeInitManagers ()
  {}

  /**
   * Callback to perform actions AFTER the managers were initialized. This
   * method is only called if initialization was successful.
   *
   * @see #beforeInitManagers()
   */
  default void afterInitManagers ()
  {}

  /**
   * @return The default backend connection state (e.g. to a database). For XML
   *         this should be TRUE for databases it should be UNDEFINED.
   * @since 5.2.4
   */
  @NonNull
  ETriState getBackendConnectionEstablishedDefaultState ();

  /**
   * @return A new SML information manager. May not be <code>null</code>.
   */
  @NonNull
  ISMLInfoManager createSMLInfoMgr ();

  /**
   * @return A new SMP settings manager. May not be <code>null</code>.
   */
  @NonNull
  ISMPSettingsManager createSettingsMgr ();

  /**
   * @return A new SMP transport profile manager. May not be <code>null</code>.
   */
  @NonNull
  ISMPTransportProfileManager createTransportProfileMgr ();

  /**
   * @return A new SMP service group manager. May not be <code>null</code>.
   */
  @NonNull
  ISMPServiceGroupManager createServiceGroupMgr ();

  /**
   * @param aIdentifierFactory
   *        The identifier factory to be used. May not be <code>null</code>.
   * @return A new SMP redirect manager. May not be <code>null</code>.
   */
  @NonNull
  ISMPRedirectManager createRedirectMgr (@NonNull IIdentifierFactory aIdentifierFactory);

  /**
   * @param aIdentifierFactory
   *        The identifier factory to be used. May not be <code>null</code>.
   * @return A new SMP service information manager. May not be
   *         <code>null</code>.
   */
  @NonNull
  ISMPServiceInformationManager createServiceInformationMgr (@NonNull IIdentifierFactory aIdentifierFactory);

  /**
   * @return A new SMP participant migration manager. May not be
   *         <code>null</code>.
   * @since 5.3.1
   */
  @NonNull
  ISMPParticipantMigrationManager createParticipantMigrationMgr ();

  /**
   * @param aIdentifierFactory
   *        The identifier factory to be used. May not be <code>null</code>.
   * @param aServiceGroupMgr
   *        The service group manager to use. May not be <code>null</code>.
   * @return A new SMP business card manager. May be <code>null</code>!
   */
  @Nullable
  ISMPBusinessCardManager createBusinessCardMgr (@NonNull IIdentifierFactory aIdentifierFactory,
                                                 @NonNull ISMPServiceGroupManager aServiceGroupMgr);

  /**
   * @param aIdentifierFactory
   *        The identifier factory to be used. May not be <code>null</code>.
   * @param aServiceGroupMgr
   *        The service group manager to use. May not be <code>null</code>.
   * @return A new SMP SPF4Peppol policy manager. May be <code>null</code>!
   * @since 8.0.13
   */
  @Nullable
  default ISMPSPF4PeppolPolicyManager createSPFPolicyMgr (@NonNull final IIdentifierFactory aIdentifierFactory,
                                                          @NonNull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return null;
  }
}
