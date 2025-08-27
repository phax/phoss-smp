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
package com.helger.phoss.smp.ui;

import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.error.IError;
import com.helger.commons.id.IHasID;
import com.helger.commons.string.StringHelper;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.photon.bootstrap4.alert.BootstrapBox;
import com.helger.photon.bootstrap4.alert.EBootstrapAlertType;
import com.helger.photon.bootstrap4.grid.BootstrapGridSpec;
import com.helger.photon.bootstrap4.pages.AbstractBootstrapWebPageForm;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.uicore.page.WebPageExecutionContext;

/**
 * Base class for form based pages
 *
 * @author Philip Helger
 * @param <DATATYPE>
 *        The handled data type.
 */
public abstract class AbstractSMPWebPageForm <DATATYPE extends IHasID <String>> extends
                                             AbstractBootstrapWebPageForm <DATATYPE, WebPageExecutionContext>
{
  /** Grid spec for identifier schemes */
  protected static final BootstrapGridSpec GS_IDENTIFIER_SCHEME = BootstrapGridSpec.create (6, 6, 6, 4, 3);
  /** Grid spec for identifier values */
  protected static final BootstrapGridSpec GS_IDENTIFIER_VALUE = BootstrapGridSpec.create (6, 6, 6, 8, 9);

  protected AbstractSMPWebPageForm (@Nonnull @Nonempty final String sID, @Nonnull final String sName)
  {
    super (sID, sName);
  }

  @Override
  protected void onInputFormError (@Nonnull final WebPageExecutionContext aWPEC,
                                   @Nonnull final FormErrorList aFormErrors)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    // Show all global errors that don't have a specific error field
    for (final IError aError : aFormErrors)
      if (StringHelper.isEmpty (aError.getErrorFieldName ()))
      {
        final EBootstrapAlertType eType = aError.isError () ? EBootstrapAlertType.DANGER : EBootstrapAlertType.WARNING;
        aNodeList.addChild (new BootstrapBox (eType).addChild (aError.getAsString (aDisplayLocale)));
      }
  }
}
