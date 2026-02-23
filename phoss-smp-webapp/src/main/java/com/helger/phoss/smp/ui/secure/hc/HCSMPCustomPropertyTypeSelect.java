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

import org.jspecify.annotations.NonNull;

import com.helger.html.hc.html.forms.HCSelect;
import com.helger.phoss.smp.domain.sgprops.ESGCustomPropertyType;
import com.helger.photon.core.form.RequestField;

/**
 * Select for the custom property type
 *
 * @author Philip Helger
 * @since 8.1.0
 */
public class HCSMPCustomPropertyTypeSelect extends HCSelect
{
  public HCSMPCustomPropertyTypeSelect (@NonNull final RequestField aRF, @NonNull final Locale aDisplayLocale)
  {
    super (aRF);

    for (final var e : ESGCustomPropertyType.values ())
      addOption (e.getID (), e.getDisplayText (aDisplayLocale));
  }
}
