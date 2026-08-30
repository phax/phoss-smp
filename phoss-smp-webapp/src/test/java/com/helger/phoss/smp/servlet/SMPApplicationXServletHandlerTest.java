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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.photon.core.csp.CSPReportingEndpoint;

/**
 * Test class for the CSP policy created by {@link SMPApplicationXServletHandler}.
 *
 * @author Philip Helger
 */
public final class SMPApplicationXServletHandlerTest
{
  private static final String NONCE = "dGVzdC1ub25jZQ==";

  @Test
  public void testPolicyWithoutReporting ()
  {
    final String sPolicy = SMPApplicationXServletHandler.createCSPPolicy (NONCE, null).getAsString ();

    assertTrue (sPolicy, sPolicy.contains ("default-src 'none'"));
    // 'self' must not be present in script-src, because 'strict-dynamic' makes it inert
    assertTrue (sPolicy, sPolicy.contains ("script-src 'nonce-" + NONCE + "' 'strict-dynamic' 'report-sample'"));
    // ... but style-src legitimately keeps it
    assertTrue (sPolicy, sPolicy.contains ("style-src 'self' 'nonce-" + NONCE + "' 'report-sample'"));
    // Only 'unsafe-inline' is meaningful in style-src-attr
    assertTrue (sPolicy, sPolicy.contains ("style-src-attr 'unsafe-inline'"));

    // The directives that do not fall back to default-src
    assertTrue (sPolicy, sPolicy.contains ("base-uri 'self'"));
    assertTrue (sPolicy, sPolicy.contains ("form-action 'self'"));
    assertTrue (sPolicy, sPolicy.contains ("frame-ancestors 'none'"));
    assertTrue (sPolicy, sPolicy.contains ("object-src 'none'"));

    // connect-src must stay 'self', otherwise the DataTables AJAX flow would break
    assertTrue (sPolicy, sPolicy.contains ("connect-src 'self'"));
    assertTrue (sPolicy, sPolicy.contains ("img-src 'self' data:"));
    assertTrue (sPolicy, sPolicy.contains ("font-src 'self'"));

    // No reporting endpoint was provided
    assertFalse (sPolicy, sPolicy.contains ("report-uri"));
    assertFalse (sPolicy, sPolicy.contains ("report-to"));
  }

  @Test
  public void testPolicyWithReporting ()
  {
    final CSPReportingEndpoint aEP = new CSPReportingEndpoint ("/smp-cspreporting?page=menuitem-service_groups_export_data&build=8.3.0");
    final String sPolicy = SMPApplicationXServletHandler.createCSPPolicy (NONCE, aEP).getAsString ();

    // Both mechanisms are emitted, and "report-to" uses the name the header declares
    assertTrue (sPolicy,
                sPolicy.contains ("report-uri /smp-cspreporting?page=menuitem-service_groups_export_data&build=8.3.0"));
    assertTrue (sPolicy, sPolicy.contains ("report-to " + CSPReportingEndpoint.DEFAULT_ENDPOINT_NAME));
    assertEquals (CSPReportingEndpoint.DEFAULT_ENDPOINT_NAME +
                  "=\"/smp-cspreporting?page=menuitem-service_groups_export_data&build=8.3.0\"",
                  aEP.getReportingEndpointsHeaderValue ());
  }
}
