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
package com.helger.peppol.smpserver.ui.pub;

import javax.annotation.Nonnull;

import com.helger.peppol.smpserver.app.CApp;
import com.helger.peppol.smpserver.ui.ajax.CAjaxPublic;
import com.helger.photon.basic.app.locale.ILocaleManager;
import com.helger.photon.basic.app.menu.IMenuTree;
import com.helger.photon.core.ajax.IAjaxInvoker;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.photon.core.app.init.DefaultApplicationInitializer;
import com.helger.photon.core.app.layout.CLayout;
import com.helger.photon.core.app.layout.ILayoutManager;

/**
 * Initialize the view application stuff
 *
 * @author Philip Helger
 */
public final class InitializerPublic extends DefaultApplicationInitializer <LayoutExecutionContext>
{
  @Override
  public void initLocales (@Nonnull final ILocaleManager aLocaleMgr)
  {
    aLocaleMgr.registerLocale (CApp.DEFAULT_LOCALE);
    aLocaleMgr.setDefaultLocale (CApp.DEFAULT_LOCALE);
  }

  @Override
  public void initLayout (@Nonnull final ILayoutManager <LayoutExecutionContext> aLayoutMgr)
  {
    // Register all layout area handler (order is important for SEO!)
    aLayoutMgr.registerAreaContentProvider (CLayout.LAYOUT_AREAID_VIEWPORT, new SMPRendererPublic ());
  }

  @Override
  public void initMenu (@Nonnull final IMenuTree aMenuTree)
  {
    MenuPublic.init (aMenuTree);
  }

  @Override
  public void initAjax (@Nonnull final IAjaxInvoker aAjaxInvoker)
  {
    aAjaxInvoker.registerFunction (CAjaxPublic.DATATABLES);
    aAjaxInvoker.registerFunction (CAjaxPublic.DATATABLES_I18N);
    aAjaxInvoker.registerFunction (CAjaxPublic.LOGIN);
  }

  @Override
  public void initRest ()
  {}
}
