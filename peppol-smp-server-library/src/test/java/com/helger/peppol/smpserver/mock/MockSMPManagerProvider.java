/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.mock;

import javax.annotation.Nonnull;

import com.helger.peppol.smpserver.domain.ISMPManagerProvider;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardManager;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.sml.ISMLInfoManager;
import com.helger.peppol.smpserver.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.settings.ISMPSettingsManager;

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
  @Nonnull
  public ISMLInfoManager createSMLInfoMgr ()
  {
    return new MockSMLInfoManager ();
  }

  @Nonnull
  public ISMPSettingsManager createSettingsMgr ()
  {
    return new MockSMPSettingsManager ();
  }

  @Nonnull
  public ISMPTransportProfileManager createTransportProfileMgr ()
  {
    return new MockSMPTransportProfileManager ();
  }

  @Nonnull
  public ISMPUserManager createUserMgr ()
  {
    return new MockSMPUserManager ();
  }

  @Nonnull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    return new MockSMPServiceGroupManager ();
  }

  @Nonnull
  public ISMPRedirectManager createRedirectMgr ()
  {
    return new MockSMPRedirectManager ();
  }

  @Nonnull
  public ISMPServiceInformationManager createServiceInformationMgr ()
  {
    return new MockSMPServiceInformationManager ();
  }

  @Nonnull
  public ISMPBusinessCardManager createBusinessCardMgr (@Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new MockSMPBusinessCardManager ();
  }
}
