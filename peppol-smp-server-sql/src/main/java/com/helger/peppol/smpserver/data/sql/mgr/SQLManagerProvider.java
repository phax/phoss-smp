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
package com.helger.peppol.smpserver.data.sql.mgr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.string.ToStringGenerator;
import com.helger.dao.DAOException;
import com.helger.peppol.smpserver.domain.ISMPManagerProvider;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardManager;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.sml.ISMLInfoManager;
import com.helger.peppol.smpserver.domain.sml.SMLInfoManager;
import com.helger.peppol.smpserver.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.peppol.smpserver.domain.transportprofile.SMPTransportProfileManager;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.settings.ISMPSettingsManager;
import com.helger.peppol.smpserver.settings.SMPSettingsManager;

/**
 * {@link ISMPManagerProvider} implementation for this backend.
 *
 * @author Philip Helger
 */
public final class SQLManagerProvider implements ISMPManagerProvider
{
  private static final String SML_INFO_XML = "sml-info.xml";
  private static final String SMP_SETTINGS_XML = "smp-settings.xml";
  private static final String SMP_TRANSPORT_PROFILES_XML = "transportprofiles.xml";

  public SQLManagerProvider ()
  {}

  // TODO currently also file based
  @Nonnull
  public ISMLInfoManager createSMLInfoMgr ()
  {
    try
    {
      return new SMLInfoManager (SML_INFO_XML);
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
      return new SMPSettingsManager (SMP_SETTINGS_XML);
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
      return new SMPTransportProfileManager (SMP_TRANSPORT_PROFILES_XML);
    }
    catch (final DAOException ex)
    {
      throw new IllegalStateException (ex.getMessage (), ex);
    }
  }

  @Nonnull
  public ISMPUserManager createUserMgr ()
  {
    return new SQLUserManager ();
  }

  @Nonnull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    return new SQLServiceGroupManager ();
  }

  @Nonnull
  public ISMPRedirectManager createRedirectMgr ()
  {
    return new SQLRedirectManager ();
  }

  @Nonnull
  public ISMPServiceInformationManager createServiceInformationMgr ()
  {
    return new SQLServiceInformationManager ();
  }

  @Nullable
  public ISMPBusinessCardManager createBusinessCardMgr (@Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    return new SQLBusinessCardManager (aServiceGroupMgr);
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).getToString ();
  }
}
