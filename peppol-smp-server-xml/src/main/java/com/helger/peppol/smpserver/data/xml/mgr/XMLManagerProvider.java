/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.data.xml.mgr;

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
public final class XMLManagerProvider implements ISMPManagerProvider
{
  public static final String SML_INFO_XML = "sml-info.xml";
  public static final String SMP_SETTINGS_XML = "smp-settings.xml";
  public static final String SMP_TRANSPORT_PROFILES_XML = "transportprofiles.xml";
  public static final String SMP_SERVICE_GROUP_XML = "smp-servicegroup.xml";
  public static final String SMP_REDIRECT_XML = "smp-redirect.xml";
  public static final String SMP_SERVICE_INFORMATION_XML = "smp-serviceinformation.xml";
  public static final String SMP_BUSINESS_CARD_XML = "smp-business-card.xml";

  public XMLManagerProvider ()
  {}

  @Nonnull
  public ISMLInfoManager createSMLInfoMgr ()
  {
    try
    {
      return new SMLInfoManager (SML_INFO_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Nonnull
  public ISMPSettingsManager createSettingsMgr ()
  {
    try
    {
      return new SMPSettingsManager (SMP_SETTINGS_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Nonnull
  public ISMPTransportProfileManager createTransportProfileMgr ()
  {
    try
    {
      return new SMPTransportProfileManager (SMP_TRANSPORT_PROFILES_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Nonnull
  public ISMPUserManager createUserMgr ()
  {
    return new XMLUserManager ();
  }

  @Nonnull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    try
    {
      return new XMLServiceGroupManager (SMP_SERVICE_GROUP_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Nonnull
  public ISMPRedirectManager createRedirectMgr ()
  {
    try
    {
      return new XMLRedirectManager (SMP_REDIRECT_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Nonnull
  public ISMPServiceInformationManager createServiceInformationMgr ()
  {
    try
    {
      return new XMLServiceInformationManager (SMP_SERVICE_INFORMATION_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Nullable
  public ISMPBusinessCardManager createBusinessCardMgr (@Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    try
    {
      return new XMLBusinessCardManager (SMP_BUSINESS_CARD_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).getToString ();
  }
}
