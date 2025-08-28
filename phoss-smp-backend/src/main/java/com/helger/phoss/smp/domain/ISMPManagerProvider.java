/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain;

import com.helger.base.state.ETriState;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.settings.ISMPSettingsManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
  @Nonnull
  ETriState getBackendConnectionEstablishedDefaultState ();

  /**
   * @return A new SML information manager. May not be <code>null</code>.
   */
  @Nonnull
  ISMLInfoManager createSMLInfoMgr ();

  /**
   * @return A new SMP settings manager. May not be <code>null</code>.
   */
  @Nonnull
  ISMPSettingsManager createSettingsMgr ();

  /**
   * @return A new SMP transport profile manager. May not be <code>null</code>.
   */
  @Nonnull
  ISMPTransportProfileManager createTransportProfileMgr ();

  /**
   * @return A new SMP service group manager. May not be <code>null</code>.
   */
  @Nonnull
  ISMPServiceGroupManager createServiceGroupMgr ();

  /**
   * @param aIdentifierFactory
   *        The identifier factory to be used. May not be <code>null</code>.
   * @return A new SMP redirect manager. May not be <code>null</code>.
   */
  @Nonnull
  ISMPRedirectManager createRedirectMgr (@Nonnull IIdentifierFactory aIdentifierFactory);

  /**
   * @param aIdentifierFactory
   *        The identifier factory to be used. May not be <code>null</code>.
   * @return A new SMP service information manager. May not be
   *         <code>null</code>.
   */
  @Nonnull
  ISMPServiceInformationManager createServiceInformationMgr (@Nonnull IIdentifierFactory aIdentifierFactory);

  /**
   * @return A new SMP participant migration manager. May not be
   *         <code>null</code>.
   * @since 5.3.1
   */
  @Nonnull
  ISMPParticipantMigrationManager createParticipantMigrationMgr ();

  /**
   * @param aIdentifierFactory
   *        The identifier factory to be used. May not be <code>null</code>.
   * @param aServiceGroupMgr
   *        The service group manager to use. May not be <code>null</code>.
   * @return A new SMP business card manager. May be <code>null</code>!
   */
  @Nullable
  ISMPBusinessCardManager createBusinessCardMgr (@Nonnull IIdentifierFactory aIdentifierFactory,
                                                 @Nonnull ISMPServiceGroupManager aServiceGroupMgr);
}
