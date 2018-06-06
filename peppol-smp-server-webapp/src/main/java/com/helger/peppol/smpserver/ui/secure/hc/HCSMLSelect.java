/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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

import java.util.Locale;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.name.IHasDisplayName;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.html.select.HCExtSelect;

public class HCSMLSelect extends HCExtSelect
{
  @Nonnull
  @Nonempty
  public static String getDisplayName (@Nonnull final ISMLInfo aObj)
  {
    return "[" + aObj.getDisplayName () + "] " + aObj.getManagementServiceURL () + " (" + aObj.getDNSZone () + ")";
  }

  public HCSMLSelect (@Nonnull final RequestField aRF,
                      @Nonnull final Locale aDisplayLocale,
                      @Nullable final Predicate <? super ISMLInfo> aFilter)
  {
    super (aRF);
    SMPMetaManager.getSMLInfoMgr ()
                  .getAllSMLInfos ()
                  .getSortedInline (IHasDisplayName.getComparatorCollating (aDisplayLocale))
                  .findAll (aFilter, x -> addOption (x.getID (), getDisplayName (x)));
    addOptionPleaseSelect (aDisplayLocale);
  }
}
