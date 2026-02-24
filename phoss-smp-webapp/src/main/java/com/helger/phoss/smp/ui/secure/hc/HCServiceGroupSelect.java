/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.html.request.IHCRequestField;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.html.select.HCExtSelect;
import com.helger.photon.uictrls.select2.HCSelect2;

import jakarta.annotation.Nullable;

/**
 * Select box for existing service groups.
 *
 * @author Philip Helger
 */
public class HCServiceGroupSelect extends HCExtSelect implements IHCServiceGroupSelect
{
  @NonNull
  @Nonempty
  public static String getDisplayName (@NonNull final ISMPServiceGroup aServiceGroup)
  {
    final String sOwnerName = SMPCommonUI.getOwnerName (aServiceGroup.getOwnerID ());
    return aServiceGroup.getParticipantIdentifier ().getURIEncoded () + " [" + sOwnerName + "]";
  }

  private static void _iterateMatchingSG (@Nullable final Predicate <? super ISMPServiceGroup> aIncludeFilter,
                                          @NonNull final Consumer <ISMPServiceGroup> aSGConsumer)
  {
    for (final ISMPServiceGroup aServiceGroup : SMPMetaManager.getServiceGroupMgr ()
                                                              .getAllSMPServiceGroups ()
                                                              .getSortedInline (ISMPServiceGroup.comparator ()))
      if (aIncludeFilter == null || aIncludeFilter.test (aServiceGroup))
        aSGConsumer.accept (aServiceGroup);
  }

  private HCServiceGroupSelect (@NonNull final RequestField aRF,
                                @NonNull final Locale aDisplayLocale,
                                @Nullable final Predicate <? super ISMPServiceGroup> aIncludeFilter)
  {
    super (aRF);

    _iterateMatchingSG (aIncludeFilter, aServiceGroup -> addOption (aServiceGroup.getID (), getDisplayName (aServiceGroup)));

    if (!hasSelectedOption ())
      addOptionPleaseSelect (aDisplayLocale);
  }

  public boolean containsAnyServiceGroup ()
  {
    return containsEffectiveOption ();
  }

  private static class MySelect2 extends HCSelect2 implements IHCServiceGroupSelect
  {
    public MySelect2 (@NonNull final IHCRequestField aRF)
    {
      super (aRF);
    }

    public boolean containsAnyServiceGroup ()
    {
      return containsEffectiveOption ();
    }
  }

  @NonNull
  public static IHCServiceGroupSelect create (@NonNull final RequestField aRF,
                                              @NonNull final Locale aDisplayLocale,
                                              @Nullable final Predicate <? super ISMPServiceGroup> aIncludeFilter,
                                              final boolean bReadOnly)
  {
    if (true)
    {
      final MySelect2 aSelect2 = new MySelect2 (aRF);
      _iterateMatchingSG (aIncludeFilter,
                          aServiceGroup -> aSelect2.addOption (aServiceGroup.getID (), getDisplayName (aServiceGroup)));
      return aSelect2;
    }

    final HCServiceGroupSelect ret = new HCServiceGroupSelect (aRF, aDisplayLocale, aIncludeFilter);
    ret.setReadOnly (bReadOnly);
    return ret;
  }
}
