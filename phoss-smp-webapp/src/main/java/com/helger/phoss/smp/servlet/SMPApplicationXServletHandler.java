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

import com.helger.http.CHttpHeader;
import com.helger.http.csp.CSPDirective;
import com.helger.http.csp.CSPPolicy;
import com.helger.http.csp.CSPSourceList;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.photon.app.csrf.CSRFSessionManager;
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

      final String sNonce = CSRFSessionManager.getInstance ().getNonce ();
      // srict-dynamic is needed for BusinessCard page, loading dynamic JS
      final CSPSourceList aScriptSrcList = new CSPSourceList ().addKeywordSelf ()
                                                               .addNonce (sNonce)
                                                               .addKeywordStrictDynamic ()
                                                               .addKeywordReportSample ();
      final CSPSourceList aStyleSrcList = new CSPSourceList ().addKeywordSelf ()
                                                              .addNonce (sNonce)
                                                              .addKeywordReportSample ();
      final CSPSourceList aStyleSrcAttrList = new CSPSourceList ().addKeywordSelf ().addKeywordUnsafeInline ();
      // Allow data images for Bootstrap 4
      final CSPSourceList aImgSrcList = new CSPSourceList ().addKeywordSelf ().addHost ("data:");
      final CSPSourceList aConnectSrcList = new CSPSourceList ().addKeywordSelf ();
      final CSPSourceList aFontSrcList = new CSPSourceList ().addKeywordSelf ();

      final CSPPolicy aPolicy = new CSPPolicy ();
      aPolicy.addDirective (CSPDirective.createDefaultSrc (new CSPSourceList ().addKeywordNone ()))
             .addDirective (CSPDirective.createScriptSrc (aScriptSrcList))
             .addDirective (CSPDirective.createStyleSrc (aStyleSrcList))
             .addDirective (CSPDirective.createStyleSrcAttr (aStyleSrcAttrList))
             .addDirective (CSPDirective.createImgSrc (aImgSrcList))
             .addDirective (CSPDirective.createConnectSrc (aConnectSrcList))
             .addDirective (CSPDirective.createFontSrc (aFontSrcList));

      if (bReporting)
      {
        // Report only if enabled - avoid spaming
        aPolicy.addDirective (CSPDirective.createReportURI (aRequestScope.getContextPath () +
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
