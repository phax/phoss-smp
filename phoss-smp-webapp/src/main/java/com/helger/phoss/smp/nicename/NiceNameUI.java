/**
 * Copyright (C) 2014-2021 Philip Helger and contributors
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.html.textlevel.HCSmall;
import com.helger.html.hc.html.textlevel.HCWBR;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.photon.bootstrap4.badge.BootstrapBadge;
import com.helger.photon.bootstrap4.badge.EBootstrapBadgeType;

/**
 * Common UI for nice names
 *
 * @author Philip Helger
 */
@Immutable
public final class NiceNameUI
{
  private NiceNameUI ()
  {}

  @Nonnull
  private static IHCNode _getWBRList (@Nonnull final String s)
  {
    final HCNodeList ret = new HCNodeList ();
    String sRest = s;
    final int nChars = 10;
    while (sRest.length () > nChars)
    {
      ret.addChild (sRest.substring (0, nChars)).addChild (new HCWBR ());
      sRest = sRest.substring (nChars);
    }
    if (sRest.length () > 0)
      ret.addChild (sRest);
    return ret;
  }

  @Nonnull
  private static IHCNode createFormattedID (@Nonnull final String sID,
                                            @Nullable final String sName,
                                            @Nullable final EBootstrapBadgeType eType,
                                            final boolean bIsDeprecated,
                                            final boolean bInDetails)
  {
    if (sName == null)
    {
      // No nice name present
      if (bInDetails)
        return new HCCode ().addChild (sID);
      return new HCTextNode (sID);
    }

    final HCNodeList ret = new HCNodeList ();
    ret.addChild (new BootstrapBadge (eType).addChild (sName));
    if (bIsDeprecated)
    {
      ret.addChild (" ").addChild (new BootstrapBadge (EBootstrapBadgeType.WARNING).addChild ("Identifier is deprecated"));
    }
    if (bInDetails)
    {
      ret.addChild (new HCSmall ().addChild (" (").addChild (new HCCode ().addChild (sID)).addChild (")"));
    }
    return ret;
  }

  @Nonnull
  private static IHCNode _createID (@Nonnull final String sID, @Nullable final NiceNameEntry aNiceName, final boolean bInDetails)
  {
    if (aNiceName == null)
      return createFormattedID (sID, null, null, false, bInDetails);
    return createFormattedID (sID, aNiceName.getName (), EBootstrapBadgeType.SUCCESS, aNiceName.isDeprecated (), bInDetails);
  }

  @Nonnull
  public static IHCNode getDocumentTypeID (@Nonnull final IDocumentTypeIdentifier aDocTypeID, final boolean bInDetails)
  {
    return _createID (aDocTypeID.getURIEncoded (), NiceNameHandler.getDocTypeNiceName (aDocTypeID), bInDetails);
  }

  @Nonnull
  public static IHCNode getProcessID (@Nonnull final IDocumentTypeIdentifier aDocTypeID,
                                      @Nonnull final IProcessIdentifier aProcessID,
                                      final boolean bInDetails)
  {
    final String sURI = aProcessID.getURIEncoded ();

    // Check direct match first
    NiceNameEntry aNN = NiceNameHandler.getProcessNiceName (sURI);
    if (aNN != null)
      return _createID (sURI, aNN, bInDetails);

    aNN = NiceNameHandler.getDocTypeNiceName (aDocTypeID);
    if (aNN != null)
    {
      if (aNN.containsProcessID (aProcessID))
        return createFormattedID (sURI, "Matching Process Identifier", EBootstrapBadgeType.SUCCESS, false, bInDetails);
      return createFormattedID (sURI, "Unexpected Process Identifier", EBootstrapBadgeType.WARNING, false, bInDetails || true);
    }
    return createFormattedID (sURI, null, null, false, bInDetails);
  }

  @Nonnull
  public static IHCNode getTransportProfile (@Nullable final String sTransportProfile, final boolean bInDetails)
  {
    final ISMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();
    final ISMPTransportProfile aTP = aTransportProfileMgr.getSMPTransportProfileOfID (sTransportProfile);
    if (aTP == null)
      return createFormattedID (sTransportProfile, null, null, false, bInDetails);
    return createFormattedID (sTransportProfile, aTP.getName (), EBootstrapBadgeType.SUCCESS, aTP.isDeprecated (), bInDetails);
  }
}
