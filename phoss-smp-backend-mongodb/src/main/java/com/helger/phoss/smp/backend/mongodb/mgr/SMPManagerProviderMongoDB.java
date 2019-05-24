/**
 * Copyright (C) 2014-2019 Philip Helger and contributors
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.string.ToStringGenerator;
import com.helger.phoss.smp.domain.ISMPManagerProvider;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.domain.user.ISMPUserManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.settings.ISMPSettingsManager;

/**
 * {@link ISMPManagerProvider} implementation for this backend.
 *
 * @author Philip Helger
 */
public final class SMPManagerProviderMongoDB implements ISMPManagerProvider
{
  public SMPManagerProviderMongoDB ()
  {}

  @Nonnull
  public ISMLInfoManager createSMLInfoMgr ()
  {
    return new SMLInfoManagerMongoDB ();
  }

  @Nonnull
  public ISMPSettingsManager createSettingsMgr ()
  {
    return new SMPSettingsManagerMongoDB ();
  }

  @Nonnull
  public ISMPTransportProfileManager createTransportProfileMgr ()
  {
    return new SMPTransportProfileManagerMongoDB ();
  }

  @Nonnull
  public ISMPUserManager createUserMgr ()
  {
    return new SMPUserManagerPhoton ();
  }

  @Nonnull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    return new SMPServiceGroupManagerMongoDB ();
  }

  @Nonnull
  public ISMPRedirectManager createRedirectMgr ()
  {
    return new SMPRedirectManagerMongoDB ();
  }

  @Nonnull
  public ISMPServiceInformationManager createServiceInformationMgr ()
  {
    return null;
  }

  @Nullable
  public ISMPBusinessCardManager createBusinessCardMgr (@Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return null;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).getToString ();
  }
}
