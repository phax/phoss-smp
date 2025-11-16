/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
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
package com.helger.phoss.smp.nicename;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.html.hc.IHCNode;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.ui.nicename.NiceNameUI;
import com.helger.peppolid.peppol.EPeppolCodeListItemState;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.photon.bootstrap4.badge.EBootstrapBadgeType;

import jakarta.annotation.Nullable;

/**
 * Common UI for nice names
 *
 * @author Philip Helger
 */
@Immutable
public final class SMPNiceNameUI
{
  private SMPNiceNameUI ()
  {}

  @NonNull
  public static IHCNode getTransportProfile (@Nullable final String sTransportProfile, final boolean bInDetails)
  {
    final ISMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();

    // This may be an SQL query
    final ISMPTransportProfile aTP = aTransportProfileMgr.getSMPTransportProfileOfID (sTransportProfile);
    return getTransportProfile (sTransportProfile, aTP, bInDetails);
  }

  @NonNull
  public static IHCNode getTransportProfile (@Nullable final String sTransportProfile,
                                             @Nullable final ISMPTransportProfile aTP,
                                             final boolean bInDetails)
  {
    if (aTP == null)
      return NiceNameUI.createFormattedID (sTransportProfile, null, null, null, bInDetails);

    // Transform from TP state to code list item state
    final EPeppolCodeListItemState eState = switch (aTP.getState ())
    {
      case ACTIVE -> EPeppolCodeListItemState.ACTIVE;
      case DEPRECATED -> EPeppolCodeListItemState.DEPRECATED;
      case DELETED -> EPeppolCodeListItemState.REMOVED;
    };
    return NiceNameUI.createFormattedID (sTransportProfile,
                                         aTP.getName (),
                                         EBootstrapBadgeType.SUCCESS,
                                         NiceNameUI.createStateBadge (eState),
                                         bInDetails);
  }
}
