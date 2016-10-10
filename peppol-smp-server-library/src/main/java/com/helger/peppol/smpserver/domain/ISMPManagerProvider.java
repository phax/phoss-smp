/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
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
