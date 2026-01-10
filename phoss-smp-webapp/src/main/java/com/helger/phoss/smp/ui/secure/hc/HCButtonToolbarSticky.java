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

import org.jspecify.annotations.NonNull;

import com.helger.html.css.DefaultCSSClassProvider;
import com.helger.html.css.ICSSClassProvider;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.core.execcontext.ILayoutExecutionContext;

/**
 * Sticky button toolbar
 * 
 * @author Philip Helger
 */
public class HCButtonToolbarSticky extends BootstrapButtonToolbar
{
  private static final ICSSClassProvider STICKY_BOTTOM = DefaultCSSClassProvider.create ("sticky-bottom");

  public HCButtonToolbarSticky (@NonNull final ILayoutExecutionContext aLEC)
  {
    super (aLEC);
    addClasses (STICKY_BOTTOM, CBootstrapCSS.BG_LIGHT, CBootstrapCSS.BORDER_TOP, CBootstrapCSS.P_3);
  }
}
