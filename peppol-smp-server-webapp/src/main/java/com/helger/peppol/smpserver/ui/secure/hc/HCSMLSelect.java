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
import javax.annotation.Nullable;

import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.sml.ESML;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.html.select.HCExtSelect;

public class HCSMLSelect extends HCExtSelect
{
  @Nullable
  public static String getSMLName (@Nonnull final ESML eSML)
  {
    if (eSML == ESML.DIGIT_PRODUCTION)
      return "SML";
    if (eSML == ESML.DIGIT_TEST)
      return "SMK";
    return "other";
  }

  @Nonnull
  private static String _getPrefix (@Nonnull final ESML eSML)
  {
    final String sName = getSMLName (eSML);
    if (StringHelper.hasText (sName))
      return "[" + sName + "] ";
    return "";
  }

  public HCSMLSelect (@Nonnull final RequestField aRF)
  {
    super (aRF);
    ArrayHelper.forEach (ESML.values (),
                         ESML::requiresClientCertificate,
                         eSML -> addOption (eSML.getID (), _getPrefix (eSML) + eSML.getManagementServiceURL ()));
  }
}
