/*
 * Copyright (C) 2014-2023 Philip Helger and contributors
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
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.helger.commons.string.StringHelper;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.metadata.HCHead;
import com.helger.html.hc.html.root.HCHtml;
import com.helger.html.hc.html.sections.HCBody;
import com.helger.phoss.smp.app.CSMP;
import com.helger.photon.core.appid.RequestSettings;
import com.helger.photon.core.execcontext.ISimpleWebExecutionContext;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.core.html.AbstractSWECHTMLProvider;
import com.helger.photon.core.interror.InternalErrorBuilder;
import com.helger.photon.core.menu.IMenuItemPage;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.forcedredirect.ForcedRedirectException;

/**
 * Main class for creating HTML output
 *
 * @author Philip Helger
 */
public class SMPLayoutHTMLProvider extends AbstractSWECHTMLProvider
{
  private final Function <LayoutExecutionContext, IHCNode> m_aFactory;

  public SMPLayoutHTMLProvider (@Nonnull final Function <LayoutExecutionContext, IHCNode> aFactory)
  {
    m_aFactory = aFactory;
  }

  @Override
  protected void fillBody (@Nonnull final ISimpleWebExecutionContext aSWEC, @Nonnull final HCHtml aHtml)
  {
    final IRequestWebScopeWithoutResponse aRequestScope = aSWEC.getRequestScope ();
    final Locale aDisplayLocale = aSWEC.getDisplayLocale ();
    final IMenuItemPage aMenuItem = RequestSettings.getMenuItem (aRequestScope);
    final LayoutExecutionContext aLEC = new LayoutExecutionContext (aSWEC, aMenuItem);
    final HCHead aHead = aHtml.head ();
    final HCBody aBody = aHtml.body ();

    // Add menu item in page title
    aHead.setPageTitle (StringHelper.getConcatenatedOnDemand (CSMP.getApplicationTitle (),
                                                              " - ",
                                                              aMenuItem.getDisplayText (aDisplayLocale)));

    try
    {
      final IHCNode aNode = m_aFactory.apply (aLEC);
      aBody.addChild (aNode);
    }
    catch (final ForcedRedirectException ex)
    {
      throw ex;
    }
    catch (final RuntimeException ex)
    {
      new InternalErrorBuilder ().setDisplayLocale (aDisplayLocale)
                                 .setRequestScope (aRequestScope)
                                 .setThrowable (ex)
                                 .setUIErrorHandlerFor (aBody)
                                 .handle ();
    }
  }
}
