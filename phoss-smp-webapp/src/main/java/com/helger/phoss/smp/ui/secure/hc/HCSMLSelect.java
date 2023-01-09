/*
 * Copyright (C) 2014-2023 Philip Helger and contributors
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

import java.util.Locale;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.name.IHasDisplayName;
import com.helger.commons.url.SimpleURL;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.phoss.smp.domain.SMPMetaManager;
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

  @Nonnull
  @Nonempty
  public static HCNodeList getDisplayNameNode (@Nonnull final ISMLInfo aObj)
  {
    return new HCNodeList ().addChild ("[" + aObj.getDisplayName () + "] ")
                            .addChild (new HCA (new SimpleURL (aObj.getManagementServiceURL ())).setTargetBlank ()
                                                                                                .addChild (aObj.getManagementServiceURL ()))
                            .addChild (" (" + aObj.getDNSZone () + ")");
  }

  public HCSMLSelect (@Nonnull final RequestField aRF,
                      @Nonnull final Locale aDisplayLocale,
                      @Nullable final Predicate <? super ISMLInfo> aFilter)
  {
    super (aRF);
    for (final ISMLInfo aItem : SMPMetaManager.getSMLInfoMgr ()
                                              .getAllSMLInfos ()
                                              .getSortedInline (IHasDisplayName.getComparatorCollating (aDisplayLocale)))
      if (aFilter == null || aFilter.test (aItem))
        addOption (aItem.getID (), getDisplayName (aItem));
    addOptionPleaseSelect (aDisplayLocale);
  }
}
