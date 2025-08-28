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
package com.helger.phoss.smp.servlet;

import com.helger.http.EHttpMethod;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.xservlet.AbstractXServlet;

/**
 * The servlet to show the application status.<br>
 * Source: https://github.com/phax/phoss-smp/issues/73
 *
 * @author Philip Helger
 * @since 5.0.6
 */
public class SMPStatusServlet extends AbstractXServlet
{
  public static final String SERVLET_DEFAULT_NAME = "smp-status";
  public static final String SERVLET_DEFAULT_PATH = '/' + SERVLET_DEFAULT_NAME;

  public SMPStatusServlet ()
  {
    handlerRegistry ().registerHandler (EHttpMethod.GET, new SMPStatusXServletHandler ());
    if (SMPWebAppConfiguration.isHttpOptionsDisabled ())
      handlerRegistry ().unregisterHandler (EHttpMethod.OPTIONS);
  }
}
