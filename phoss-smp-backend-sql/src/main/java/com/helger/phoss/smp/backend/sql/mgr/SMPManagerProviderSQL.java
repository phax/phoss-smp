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
package com.helger.phoss.smp.backend.sql.mgr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.string.ToStringGenerator;
import com.helger.dao.DAOException;
import com.helger.phoss.smp.domain.ISMPManagerProvider;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.domain.sml.SMLInfoManagerXML;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.domain.transportprofile.SMPTransportProfileManagerXML;
import com.helger.phoss.smp.domain.user.ISMPUserManager;
import com.helger.phoss.smp.settings.ISMPSettingsManager;
import com.helger.phoss.smp.settings.SMPSettingsManagerXML;

/**
 * {@link ISMPManagerProvider} implementation for this backend.
 *
 * @author Philip Helger
 */
public final class SMPManagerProviderSQL implements ISMPManagerProvider
{
  private static final String SML_INFO_XML = "sml-info.xml";
  private static final String SMP_SETTINGS_XML = "smp-settings.xml";
  private static final String SMP_TRANSPORT_PROFILES_XML = "transportprofiles.xml";

  public SMPManagerProviderSQL ()
  {}

  // TODO currently also file based
  @Nonnull
  public ISMLInfoManager createSMLInfoMgr ()
  {
    try
    {
      return new SMLInfoManagerXML (SML_INFO_XML);
    }
    catch (final DAOException ex)
    {
      throw new IllegalStateException (ex.getMessage (), ex);
    }
  }

  // TODO currently also file based
  @Nonnull
  public ISMPSettingsManager createSettingsMgr ()
  {
    try
    {
      return new SMPSettingsManagerXML (SMP_SETTINGS_XML);
    }
    catch (final DAOException ex)
    {
      throw new IllegalStateException (ex.getMessage (), ex);
    }
  }

  // TODO currently also file based
  @Nonnull
  public ISMPTransportProfileManager createTransportProfileMgr ()
  {
    try
    {
      return new SMPTransportProfileManagerXML (SMP_TRANSPORT_PROFILES_XML);
    }
    catch (final DAOException ex)
    {
      throw new IllegalStateException (ex.getMessage (), ex);
    }
  }

  @Nonnull
  public ISMPUserManager createUserMgr ()
  {
    return new SMPUserManagerSQL ();
  }

  @Nonnull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    return new SMPServiceGroupManagerSQL ();
  }

  @Nonnull
  public ISMPRedirectManager createRedirectMgr ()
  {
    return new SMPRedirectManagerSQL ();
  }

  @Nonnull
  public ISMPServiceInformationManager createServiceInformationMgr ()
  {
    return new SMPServiceInformationManagerSQL ();
  }

  @Nullable
  public ISMPBusinessCardManager createBusinessCardMgr (@Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new SMPBusinessCardManagerSQL (aServiceGroupMgr);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).getToString ();
  }
}
