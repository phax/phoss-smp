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
package com.helger.peppol.smpserver.servlet;

import javax.annotation.Nonnull;

import com.helger.peppol.smpserver.ui.AppLayoutHTMLProvider;
import com.helger.photon.core.app.html.IHTMLProvider;
import com.helger.photon.core.servlet.AbstractSecureApplicationServlet;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * The servlet to show the secure application
 *
 * @author Philip Helger
 */
public class SecureApplicationServlet extends AbstractSecureApplicationServlet
{
  @Override
  @Nonnull
  protected IHTMLProvider createHTMLProvider (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope)
  {
    return new AppLayoutHTMLProvider ();
  }
}
