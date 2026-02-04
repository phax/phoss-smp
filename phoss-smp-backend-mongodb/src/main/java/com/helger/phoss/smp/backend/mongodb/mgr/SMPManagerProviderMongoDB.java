/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.mongodb.mgr;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.state.ETriState;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.backend.mongodb.PhotonSecurityManagerFactoryMongoDB;
import com.helger.phoss.smp.domain.ISMPManagerProvider;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.settings.ISMPSettingsManager;
import com.helger.photon.security.mgr.PhotonSecurityManager;

/**
 * {@link ISMPManagerProvider} implementation for this backend.
 *
 * @author Philip Helger
 */
public final class SMPManagerProviderMongoDB implements ISMPManagerProvider
{
  @Override
  public void beforeInitManagers ()
  {
    // Set the special PhotonSecurityManager factory
    PhotonSecurityManager.setFactory (new PhotonSecurityManagerFactoryMongoDB ());
    PhotonSecurityManager.getInstance ();
  }

  @NonNull
  public ETriState getBackendConnectionEstablishedDefaultState ()
  {
    return ETriState.UNDEFINED;
  }

  @NonNull
  public ISMLInfoManager createSMLInfoMgr ()
  {
    return new SMLInfoManagerMongoDB ();
  }

  @NonNull
  public ISMPSettingsManager createSettingsMgr ()
  {
    return new SMPSettingsManagerMongoDB ();
  }

  @NonNull
  public ISMPTransportProfileManager createTransportProfileMgr ()
  {
    return new SMPTransportProfileManagerMongoDB ();
  }

  @NonNull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    return new SMPServiceGroupManagerMongoDB ();
  }

  @NonNull
  public ISMPRedirectManager createRedirectMgr (@NonNull final IIdentifierFactory aIdentifierFactory)
  {
    return new SMPRedirectManagerMongoDB (aIdentifierFactory);
  }

  @NonNull
  public ISMPServiceInformationManager createServiceInformationMgr (@NonNull final IIdentifierFactory aIdentifierFactory)
  {
    return new SMPServiceInformationManagerMongoDB (aIdentifierFactory);
  }

  @NonNull
  public ISMPParticipantMigrationManager createParticipantMigrationMgr ()
  {
    return new SMPParticipantMigrationManagerMongoDB ();
  }

  @Nullable
  public ISMPBusinessCardManager createBusinessCardMgr (@NonNull final IIdentifierFactory aIdentifierFactory,
                                                        @NonNull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new SMPBusinessCardManagerMongoDB (aIdentifierFactory);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).getToString ();
  }
}
