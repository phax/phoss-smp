/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.ui.secure.hc;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.id.ComparatorHasIDString;
import com.helger.html.hc.html.forms.HCSelect;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.photon.core.form.RequestField;

/**
 * Select for the default transport profiles
 *
 * @author Philip Helger
 */
public class HCSMPTransportProfileSelect extends HCSelect
{
  @Nonnull
  @Nonempty
  public static String getDisplayName (@Nonnull final ISMPTransportProfile aTP)
  {
    return aTP.getID () + " [" + aTP.getName () + "]";
  }

  public HCSMPTransportProfileSelect (@Nonnull final RequestField aRF)
  {
    super (aRF);

    for (final ISMPTransportProfile aTP : CollectionHelper.getSorted (SMPMetaManager.getTransportProfileMgr ().getAllSMPTransportProfiles (),
                                                                      new ComparatorHasIDString <ISMPTransportProfile> ()))
      addOption (aTP.getID (), getDisplayName (aTP));
  }
}
