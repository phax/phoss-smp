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

import com.helger.annotation.Nonempty;
import com.helger.base.name.IHasDisplayName;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.uicore.html.select.HCExtSelect;
import com.helger.text.compare.ComparatorHelper;

import jakarta.annotation.Nonnull;

/**
 * Select box for active SMP users (depending on the backend).
 *
 * @author Philip Helger
 */
public class HCUserSelect extends HCExtSelect
{
  @Nonnull
  @Nonempty
  public static String getDisplayName (@Nonnull final IUser aUser)
  {
    return aUser.getDisplayName ();
  }

  public HCUserSelect (@Nonnull final RequestField aRF, @Nonnull final Locale aDisplayLocale)
  {
    super (aRF);

    // Use active users only
    for (final IUser aUser : PhotonSecurityManager.getUserMgr ()
                                                  .getAllActiveUsers ()
                                                  .getSortedInline (ComparatorHelper.getComparatorCollating (IHasDisplayName::getDisplayName,
                                                                                                             aDisplayLocale)))
      addOption (aUser.getID (), getDisplayName (aUser));

    if (!hasSelectedOption ())
      addOptionPleaseSelect (aDisplayLocale);
  }
}
