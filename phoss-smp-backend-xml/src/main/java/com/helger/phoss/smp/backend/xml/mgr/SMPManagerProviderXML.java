/*
 * Copyright (C) 2015-2021 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.xml.mgr;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.state.ETriState;
import com.helger.commons.string.ToStringGenerator;
import com.helger.dao.DAOException;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.domain.ISMPManagerProvider;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.pmigration.SMPParticipantMigrationManagerXML;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.domain.sml.SMLInfoManagerXML;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.domain.transportprofile.SMPTransportProfileManagerXML;
import com.helger.phoss.smp.settings.ISMPSettingsManager;
import com.helger.phoss.smp.settings.SMPSettingsManagerXML;

/**
 * {@link ISMPManagerProvider} implementation for this backend.
 *
 * @author Philip Helger
 */
public final class SMPManagerProviderXML implements ISMPManagerProvider
{
  public static final String SML_INFO_XML = "sml-info.xml";
  public static final String SMP_SETTINGS_XML = "smp-settings.xml";
  public static final String SMP_TRANSPORT_PROFILES_XML = "transportprofiles.xml";
  public static final String SMP_SERVICE_GROUP_XML = "smp-servicegroup.xml";
  public static final String SMP_REDIRECT_XML = "smp-redirect.xml";
  public static final String SMP_SERVICE_INFORMATION_XML = "smp-serviceinformation.xml";
  public static final String SMP_PARTICIPANT_MIGRATION_XML = "smp-participant-migration.xml";
  public static final String SMP_BUSINESS_CARD_XML = "smp-business-card.xml";

  public SMPManagerProviderXML ()
  {}

  @Nonnull
  public ETriState getBackendConnectionEstablishedDefaultState ()
  {
    return ETriState.TRUE;
  }

  @Nonnull
  public ISMLInfoManager createSMLInfoMgr ()
  {
    try
    {
      return new SMLInfoManagerXML (SML_INFO_XML);
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
      return new SMPSettingsManagerXML (SMP_SETTINGS_XML);
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
      return new SMPTransportProfileManagerXML (SMP_TRANSPORT_PROFILES_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Nonnull
  public ISMPServiceGroupManager createServiceGroupMgr ()
  {
    try
    {
      return new SMPServiceGroupManagerXML (SMP_SERVICE_GROUP_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Nonnull
  public ISMPRedirectManager createRedirectMgr (@Nonnull final IIdentifierFactory aIdentifierFactory,
                                                @Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    try
    {
      return new SMPRedirectManagerXML (SMP_REDIRECT_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Nonnull
  public ISMPServiceInformationManager createServiceInformationMgr (@Nonnull final IIdentifierFactory aIdentifierFactory,
                                                                    @Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    try
    {
      return new SMPServiceInformationManagerXML (SMP_SERVICE_INFORMATION_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Nonnull
  public ISMPParticipantMigrationManager createParticipantMigrationMgr ()
  {
    try
    {
      return new SMPParticipantMigrationManagerXML (SMP_PARTICIPANT_MIGRATION_XML);
    }
    catch (final DAOException ex)
    {
      throw new RuntimeException (ex.getMessage (), ex);
    }
  }

  @Nullable
  public ISMPBusinessCardManager createBusinessCardMgr (@Nonnull final IIdentifierFactory aIdentifierFactory,
                                                        @Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    try
    {
      return new SMPBusinessCardManagerXML (SMP_BUSINESS_CARD_XML);
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
