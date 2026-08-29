/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;

import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.http.CHttpHeader;
import com.helger.http.csp.CSPDirective;
import com.helger.http.csp.CSPPolicy;
import com.helger.http.csp.CSPSourceList;
import com.helger.base.string.StringHelper;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.photon.app.csrf.CSRFSessionManager;
import com.helger.photon.core.csp.CSPReportingEndpoint;
import com.helger.photon.core.csp.ICSPReportingParameterProvider;
import com.helger.photon.core.appid.RequestSettings;
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
  /** The query parameter naming the page a CSP violation occurred on */
  public static final String CSP_PARAM_PAGE = "page";
  /** The query parameter naming the build the CSP violation occurred with */
  public static final String CSP_PARAM_BUILD = "build";

  /**
   * Contributes the query parameters of the CSP report URI. Only non-sensitive values are allowed
   * here, because the report URI is part of the policy - it is readable by everybody loading the
   * page and it is echoed back in the "original-policy" field of every report.
   */
  public static final ICSPReportingParameterProvider CSP_PARAM_PROVIDER = (aRequestScope, aTarget) -> {
    final String sMenuItemID = RequestSettings.getMenuItemID (aRequestScope);
    if (StringHelper.isNotEmpty (sMenuItemID))
      aTarget.put (CSP_PARAM_PAGE, sMenuItemID);
    aTarget.put (CSP_PARAM_BUILD, CSMPServer.getVersionNumber ());
  };

  /**
   * Create the CSP policy of a secure page.
   *
   * @param sNonce
   *        The nonce of the current request. May neither be <code>null</code> nor empty.
   * @param aReportingEndpoint
   *        The reporting endpoint to be referenced, or <code>null</code> if reporting is disabled.
   * @return The created policy. Never <code>null</code>.
   */
  @NonNull
  public static CSPPolicy createCSPPolicy (@NonNull @Nonempty final String sNonce,
                                           @Nullable final CSPReportingEndpoint aReportingEndpoint)
  {
    // srict-dynamic is needed for BusinessCard page, loading dynamic JS
      // Note: with 'strict-dynamic' present, all host and scheme sources - including 'self' - are
      // ignored by the browser, so adding 'self' here would only imply a fallback that does not exist
      final CSPSourceList aScriptSrcList = new CSPSourceList ().addNonce (sNonce)
                                                               .addKeywordStrictDynamic ()
                                                               .addKeywordReportSample ();
      final CSPSourceList aStyleSrcList = new CSPSourceList ().addKeywordSelf ()
                                                              .addNonce (sNonce)
                                                              .addKeywordReportSample ();
      // Only 'unsafe-inline' and hashes are meaningful in style-src-attr
      final CSPSourceList aStyleSrcAttrList = new CSPSourceList ().addKeywordUnsafeInline ();
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
             .addDirective (CSPDirective.createFontSrc (aFontSrcList))
             // Neither form-action nor frame-ancestors fall back to default-src, so they must be
             // set explicitly
             .addDirective (CSPDirective.createBaseURI (new CSPSourceList ().addKeywordSelf ().getAsString ()))
             .addDirective (CSPDirective.createFormAction (new CSPSourceList ().addKeywordSelf ()))
             .addDirective (CSPDirective.createFrameAncestors (new CSPSourceList ().addKeywordNone ()))
             .addDirective (CSPDirective.createObjectSrc (new CSPSourceList ().addKeywordNone ()));

      if (aReportingEndpoint != null)
      {
        // "report-uri" is deprecated, but still the only mechanism some browsers honour, so both
        // are emitted
        aPolicy.addDirective (aReportingEndpoint.getAsReportURIDirective ())
               .addDirective (aReportingEndpoint.getAsReportToDirective ());
      }
    return aPolicy;
  }

  @Override
  public void handleRequest (final IRequestWebScopeWithoutResponse aRequestScope,
                             final UnifiedResponse aUnifiedResponse) throws IOException, ServletException
  {
    if (SMPWebAppConfiguration.isCSPEnabled ())
    {
      final boolean bReportingOnly = SMPWebAppConfiguration.isCSPReportingOnly ();
      final boolean bReporting = bReportingOnly || SMPWebAppConfiguration.isCSPReportingEnabled ();

      CSPReportingEndpoint aEndpoint = null;
      if (bReporting)
      {
        // Report only if enabled - avoid spaming
        final ICommonsOrderedMap <String, String> aParams = new CommonsLinkedHashMap <> ();
        CSP_PARAM_PROVIDER.addReportingParameters (aRequestScope, aParams);

        aEndpoint = new CSPReportingEndpoint (CSPReportingEndpoint.createURI (aRequestScope.getContextPath () +
                                                                             SMPCSPReportingServlet.SERVLET_DEFAULT_PATH,
                                                                             aParams));
        // The named endpoint of "report-to" is declared in this header
        aUnifiedResponse.addCustomResponseHeader (CHttpHeader.REPORTING_ENDPOINTS,
                                                  aEndpoint.getReportingEndpointsHeaderValue ());
      }

      final CSPPolicy aPolicy = createCSPPolicy (CSRFSessionManager.getInstance ().getNonce (), aEndpoint);

      // Default
      aUnifiedResponse.addCustomResponseHeader (bReportingOnly ? CHttpHeader.CONTENT_SECURITY_POLICY_REPORT_ONLY
                                                               : CHttpHeader.CONTENT_SECURITY_POLICY,
                                                aPolicy.getAsString ());
    }
    super.handleRequest (aRequestScope, aUnifiedResponse);
  }
}
