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
package com.helger.peppol.smpserver.ui;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.photon.bootstrap3.grid.BootstrapGridSpec;
import com.helger.photon.bootstrap3.pages.AbstractBootstrapWebPageForm;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public abstract class AbstractSMPWebPageForm <DATATYPE extends IHasID <String>> extends AbstractBootstrapWebPageForm <DATATYPE, WebPageExecutionContext>
{
  /** Grid spec for identifier schemes */
  protected static final BootstrapGridSpec GS_IDENTIFIER_SCHEME = BootstrapGridSpec.create (6, 6, 4, 3);
  /** Grid spec for identifier values */
  protected static final BootstrapGridSpec GS_IDENTIFIER_VALUE = BootstrapGridSpec.create (6, 6, 8, 9);

  public AbstractSMPWebPageForm (@Nonnull @Nonempty final String sID, @Nonnull final String sName)
  {
    super (sID, sName);
  }
}
