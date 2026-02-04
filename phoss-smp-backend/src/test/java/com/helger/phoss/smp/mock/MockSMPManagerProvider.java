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
package com.helger.phoss.smp.mock;

import org.jspecify.annotations.NonNull;

import com.helger.base.state.ETriState;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.domain.ISMPManagerProvider;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.settings.ISMPSettingsManager;

/**
 * This {@link ISMPManagerProvider} implementation returns non-<code>null</code>
 * managers that all do nothing. This is only needed to access the identifier
 * factory.<br>
 * Note: this class must be public
 *
 * @author Philip Helger
 */
public final class MockSMPManagerProvider implements ISMPManagerProvider
{
  @NonNull
  public ETriState getBackendConnectionEstablishedDefaultState ()
  {
    return ETriState.TRUE;
  }

  @NonNull
  public ISMLInfoManager createSMLInfoMgr ()
  {
    return new MockSMLInfoManager ();
  }

  @NonNull
  public ISMPSettingsManager createSettingsMgr ()
  {
    return new MockSMPSettingsManager ();
  }

  @NonNull
  public ISMPTransportProfileManager createTransportProfileMgr ()
  {
    return new MockSMPTransportProfileManager ();
  }

  @NonNull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    return new MockSMPServiceGroupManager ();
  }

  @NonNull
  public ISMPRedirectManager createRedirectMgr (@NonNull final IIdentifierFactory aIdentifierFactory)
  {
    return new MockSMPRedirectManager ();
  }

  @NonNull
  public ISMPServiceInformationManager createServiceInformationMgr (@NonNull final IIdentifierFactory aIdentifierFactory)
  {
    return new MockSMPServiceInformationManager ();
  }

  @NonNull
  public ISMPParticipantMigrationManager createParticipantMigrationMgr ()
  {
    return new MockSMPParticipantMigrationManager ();
  }

  @NonNull
  public ISMPBusinessCardManager createBusinessCardMgr (@NonNull final IIdentifierFactory aIdentifierFactory,
                                                        @NonNull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new MockSMPBusinessCardManager ();
  }
}
