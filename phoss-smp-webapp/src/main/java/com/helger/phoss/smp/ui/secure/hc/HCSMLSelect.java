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

import java.util.Locale;
import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.name.IHasDisplayName;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.html.select.HCExtSelect;
import com.helger.text.compare.ComparatorHelper;
import com.helger.url.SimpleURL;

import jakarta.annotation.Nullable;

public class HCSMLSelect extends HCExtSelect
{
  @NonNull
  @Nonempty
  public static String getDisplayName (@NonNull final ISMLInfo aObj)
  {
    return "[" + aObj.getDisplayName () + "] " + aObj.getManagementServiceURL () + " (" + aObj.getDNSZone () + ")";
  }

  @NonNull
  @Nonempty
  public static HCNodeList getDisplayNameNode (@NonNull final ISMLInfo aObj)
  {
    return new HCNodeList ().addChild ("[" + aObj.getDisplayName () + "] ")
                            .addChild (new HCA (new SimpleURL (aObj.getManagementServiceURL ())).setTargetBlank ()
                                                                                                .addChild (aObj.getManagementServiceURL ()))
                            .addChild (" (" + aObj.getDNSZone () + ")");
  }

  public HCSMLSelect (@NonNull final RequestField aRF,
                      @NonNull final Locale aDisplayLocale,
                      @Nullable final Predicate <? super ISMLInfo> aFilter)
  {
    super (aRF);
    for (final ISMLInfo aItem : SMPMetaManager.getSMLInfoMgr ()
                                              .getAllSMLInfos ()
                                              .getSortedInline (ComparatorHelper.getComparatorCollating (IHasDisplayName::getDisplayName,
                                                                                                         aDisplayLocale)))
      if (aFilter == null || aFilter.test (aItem))
        addOption (aItem.getID (), getDisplayName (aItem));
    addOptionPleaseSelect (aDisplayLocale);
  }
}
