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

import com.helger.annotation.Nonempty;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.html.select.HCExtSelect;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Select box for existing service groups.
 *
 * @author Philip Helger
 */
public class HCServiceGroupSelect extends HCExtSelect
{
  @Nonnull
  @Nonempty
  public static String getDisplayName (@Nonnull final ISMPServiceGroup aServiceGroup)
  {
    final String sOwnerName = SMPCommonUI.getOwnerName (aServiceGroup.getOwnerID ());
    return aServiceGroup.getParticipantIdentifier ().getURIEncoded () + " [" + sOwnerName + "]";
  }

  public HCServiceGroupSelect (@Nonnull final RequestField aRF, @Nonnull final Locale aDisplayLocale)
  {
    this (aRF, aDisplayLocale, null);
  }

  public HCServiceGroupSelect (@Nonnull final RequestField aRF,
                               @Nonnull final Locale aDisplayLocale,
                               @Nullable final Predicate <? super ISMPServiceGroup> aFilter)
  {
    super (aRF);

    for (final ISMPServiceGroup aServiceGroup : SMPMetaManager.getServiceGroupMgr ()
                                                              .getAllSMPServiceGroups ()
                                                              .getSortedInline (ISMPServiceGroup.comparator ()))
      if (aFilter == null || aFilter.test (aServiceGroup))
        addOption (aServiceGroup.getID (), getDisplayName (aServiceGroup));

    if (!hasSelectedOption ())
      addOptionPleaseSelect (aDisplayLocale);
  }

  public boolean containsAnyServiceGroup ()
  {
    return containsEffectiveOption ();
  }
}
