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
package com.helger.phoss.smp.ui.secure.hc;

import java.util.Comparator;
import java.util.Locale;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.name.IHasName;
import com.helger.html.hc.html.forms.HCSelect;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.photon.core.form.RequestField;

/**
 * Select for the default transport profiles
 *
 * @author Philip Helger
 */
public class HCSMPTransportProfileSelect extends HCSelect
{
  @NonNull
  @Nonempty
  public static String getDisplayName (@NonNull final ISMPTransportProfile aTP, @NonNull final Locale aDisplayLocale)
  {
    final String sSuffix = aTP.getState ().isActive () ? ""
                                                       : " [" + aTP.getState ().getDisplayText (aDisplayLocale) + "]";
    return aTP.getName () + " (" + aTP.getID () + ")" + sSuffix;
  }

  public HCSMPTransportProfileSelect (@NonNull final RequestField aRF, @NonNull final Locale aDisplayLocale)
  {
    super (aRF);

    for (final ISMPTransportProfile aTP : SMPMetaManager.getTransportProfileMgr ()
                                                        .getAllSMPTransportProfiles ()
                                                        .getSortedInline (Comparator.comparing (IHasName::getName)))
      addOption (aTP.getID (), getDisplayName (aTP, aDisplayLocale));
  }
}
