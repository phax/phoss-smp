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

import com.helger.annotation.Nonempty;
import com.helger.base.id.IHasID;
import com.helger.photon.bootstrap4.pages.AbstractBootstrapWebPageSimpleForm;
import com.helger.photon.uicore.page.WebPageExecutionContext;

import jakarta.annotation.Nonnull;

/**
 * Base class for simple form pages.
 *
 * @author Philip Helger
 * @param <DATATYPE>
 *        The handled data type
 */
public abstract class AbstractSMPWebPageSimpleForm <DATATYPE extends IHasID <String>> extends
                                                   AbstractBootstrapWebPageSimpleForm <DATATYPE, WebPageExecutionContext>
{
  protected AbstractSMPWebPageSimpleForm (@Nonnull @Nonempty final String sID, @Nonnull final String sName)
  {
    super (sID, sName);
  }
}
