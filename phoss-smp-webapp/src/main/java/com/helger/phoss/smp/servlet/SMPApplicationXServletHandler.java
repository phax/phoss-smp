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

import java.io.IOException;

import com.helger.commons.http.CHttpHeader;
import com.helger.http.csp.CSP2Directive;
import com.helger.http.csp.CSP2Policy;
import com.helger.http.csp.CSP2SourceList;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.photon.core.servlet.AbstractApplicationXServletHandler;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.servlet.ServletException;

/**
 * CSP enabled application servlet handler
 *
 * @author Philip Helger
 * @since 5.2.6
 */
public abstract class SMPApplicationXServletHandler extends AbstractApplicationXServletHandler
{
  @Override
  public void handleRequest (final IRequestWebScopeWithoutResponse aRequestScope,
                             final UnifiedResponse aUnifiedResponse) throws IOException, ServletException
  {
    if (SMPWebAppConfiguration.isCSPEnabled ())
    {
      final boolean bReportingOnly = SMPWebAppConfiguration.isCSPReportingOnly ();
      final boolean bReporting = bReportingOnly || SMPWebAppConfiguration.isCSPReportingEnabled ();

      final CSP2SourceList aScriptSrcList = new CSP2SourceList ().addKeywordSelf ().addKeywordUnsafeInline ();
      final CSP2SourceList aStyleSrcList = new CSP2SourceList ().addKeywordSelf ().addKeywordUnsafeInline ();
      // Allow data images for Bootstrap 4
      final CSP2SourceList aImgSrcList = new CSP2SourceList ().addKeywordSelf ().addHost ("data:");
      final CSP2SourceList aConnectSrcList = new CSP2SourceList ().addKeywordSelf ();
      final CSP2SourceList aFontSrcList = new CSP2SourceList ().addKeywordSelf ();

      final CSP2Policy aPolicy = new CSP2Policy ();
      aPolicy.addDirective (CSP2Directive.createDefaultSrc (new CSP2SourceList ().addKeywordNone ()))
             .addDirective (CSP2Directive.createScriptSrc (aScriptSrcList))
             .addDirective (CSP2Directive.createStyleSrc (aStyleSrcList))
             .addDirective (CSP2Directive.createImgSrc (aImgSrcList))
             .addDirective (CSP2Directive.createConnectSrc (aConnectSrcList))
             .addDirective (CSP2Directive.createFontSrc (aFontSrcList));

      if (bReporting)
      {
        // Report only if enabled - avoid spaming
        aPolicy.addDirective (CSP2Directive.createReportURI (aRequestScope.getContextPath () +
                                                             SMPCSPReportingServlet.SERVLET_DEFAULT_PATH));
      }

      // Default
      aUnifiedResponse.addCustomResponseHeader (bReportingOnly ? CHttpHeader.CONTENT_SECURITY_POLICY_REPORT_ONLY
                                                               : CHttpHeader.CONTENT_SECURITY_POLICY,
                                                aPolicy.getAsString ());
      // IE specific
      aUnifiedResponse.addCustomResponseHeader (bReportingOnly ? CHttpHeader.X_CONTENT_SECURITY_POLICY_REPORT_ONLY
                                                               : CHttpHeader.X_CONTENT_SECURITY_POLICY,
                                                aPolicy.getAsString ());

    }
    super.handleRequest (aRequestScope, aUnifiedResponse);
  }
}
