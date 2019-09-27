/**
 * Copyright (C) 2014-2019 Philip Helger and contributors
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
import com.helger.html.hc.html.textlevel.HCWBR;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;
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
  private static IHCNode _createID (@Nonnull final String sID, @Nullable final NiceNameEntry aNiceName)
  {
    final HCNodeList ret = new HCNodeList ();
    if (aNiceName == null)
    {
      // No nice name present
      ret.addChild (new BootstrapBadge (EBootstrapBadgeType.WARNING).addChild ("Non-standard identifier"));
    }
    else
    {
      ret.addChild (new BootstrapBadge (EBootstrapBadgeType.SUCCESS).addChild (aNiceName.getName ()));
      if (aNiceName.isDeprecated ())
      {
        ret.addChild (" ")
           .addChild (new BootstrapBadge (EBootstrapBadgeType.WARNING).addChild ("Identifier is deprecated"));
      }
    }
    ret.addChild (" ").addChild (new HCCode ().addChild (_getWBRList (sID)));
    return ret;
  }

  @Nonnull
  public static IHCNode getDocumentTypeID (@Nonnull final IDocumentTypeIdentifier aDocTypeID)
  {
    final String sURI = aDocTypeID.getURIEncoded ();
    return _createID (sURI, NiceNameHandler.getDocTypeNiceName (sURI));
  }

  @Nonnull
  public static IHCNode getProcessID (@Nonnull final IProcessIdentifier aProcessID)
  {
    final String sURI = aProcessID.getURIEncoded ();
    return _createID (sURI, NiceNameHandler.getProcessNiceName (sURI));
  }
}
