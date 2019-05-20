/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
import com.helger.phoss.smp.settings.ISMPSettingsManager;

/**
 * {@link ISMPManagerProvider} implementation for this backend.
 *
 * @author Philip Helger
 */
public final class MongoDBManagerProvider implements ISMPManagerProvider
{
  public MongoDBManagerProvider ()
  {}

  @Nonnull
  public ISMLInfoManager createSMLInfoMgr ()
  {
    return new MongoDBSMLInfoManager ();
  }

  @Nonnull
  public ISMPSettingsManager createSettingsMgr ()
  {
    return null;
  }

  @Nonnull
  public ISMPTransportProfileManager createTransportProfileMgr ()
  {
    return null;
  }

  @Nonnull
  public ISMPUserManager createUserMgr ()
  {
    return null;
  }

  @Nonnull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    return null;
  }

  @Nonnull
  public ISMPRedirectManager createRedirectMgr ()
  {
    return null;
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
