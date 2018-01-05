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
package com.helger.peppol.smpserver.domain;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardManager;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.sml.ISMLInfoManager;
import com.helger.peppol.smpserver.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.settings.ISMPSettingsManager;

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
   * @return A new SMP user manager. May not be <code>null</code>.
   */
  @Nonnull
  ISMPUserManager createUserMgr ();

  /**
   * @return A new SMP service group manager. May not be <code>null</code>.
   */
  @Nonnull
  ISMPServiceGroupManager createServiceGroupMgr ();

  /**
   * @return A new SMP redirect manager. May not be <code>null</code>.
   */
  @Nonnull
  ISMPRedirectManager createRedirectMgr ();

  /**
   * @return A new SMP service information manager. May not be
   *         <code>null</code>.
   */
  @Nonnull
  ISMPServiceInformationManager createServiceInformationMgr ();

  /**
   * @param aServiceGroupMgr
   *        The service group manager to use. May not be <code>null</code>.
   * @return A new SMP business card manager. May be <code>null</code>!
   */
  @Nullable
  ISMPBusinessCardManager createBusinessCardMgr (@Nonnull ISMPServiceGroupManager aServiceGroupMgr);
}
