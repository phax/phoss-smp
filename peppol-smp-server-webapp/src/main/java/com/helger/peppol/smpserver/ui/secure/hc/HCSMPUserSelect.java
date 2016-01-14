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

import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.commons.collection.CollectionHelper;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.user.ComparatorSMPUser;
import com.helger.peppol.smpserver.domain.user.ISMPUser;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.html.select.HCExtSelect;

/**
 * Select box for active SMP users (depending on the backend).
 *
 * @author Philip Helger
 */
public class HCSMPUserSelect extends HCExtSelect
{
  public HCSMPUserSelect (@Nonnull final RequestField aRF, @Nonnull final Locale aDisplayLocale)
  {
    super (aRF);

    for (final ISMPUser aUser : CollectionHelper.getSorted (SMPMetaManager.getUserMgr ().getAllUsers (), new ComparatorSMPUser (aDisplayLocale)))
      addOption (aUser.getID (), aUser.getUserName ());

    if (!hasSelectedOption ())
      addOptionPleaseSelect (aDisplayLocale);
  }
}
