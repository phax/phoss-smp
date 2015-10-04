/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.ui.secure;

import javax.annotation.Nonnull;

import com.helger.html.hc.html.forms.HCSelect;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.photon.core.form.RequestField;

public class SMPTransportProfileSelect extends HCSelect
{
  public SMPTransportProfileSelect (@Nonnull final RequestField aRF)
  {
    super (aRF);

    for (final ESMPTransportProfile eTP : ESMPTransportProfile.values ())
      addOption (eTP.getID (), eTP.getID () + " [" + eTP.getName () + "]");
  }
}
