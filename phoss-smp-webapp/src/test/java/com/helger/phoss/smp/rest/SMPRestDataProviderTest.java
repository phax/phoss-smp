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
package com.helger.phoss.smp.rest;

import static org.junit.Assert.assertEquals;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsMap;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.config.source.appl.ConfigurationSourceFunction;
import com.helger.phoss.smp.config.SMPConfig;
import com.helger.phoss.smp.config.SMPConfigProvider;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.servlet.ServletContextPathHolder;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.servlet.mock.MockHttpServletResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.web.scope.impl.RequestWebScope;

import jakarta.annotation.Nullable;

/**
 * Test class for class {@link SMPRestDataProvider}.
 *
 * @author Philip Helger
 */
public final class SMPRestDataProviderTest
{
  private static final String CONTEXT_PATH = "/smp";
  private static final String REQUEST_URL = "http://internal.host:90" + CONTEXT_PATH + "/iso6523-actorid-upis%3A%3A9915%3Atest";

  private IConfigWithFallback m_aOldConfig;

  @Before
  public void before ()
  {
    final ICommonsMap <String, String> aConfigValues = new CommonsHashMap <> ();
    aConfigValues.put (SMPServerConfiguration.KEY_SMP_PUBLIC_URL_MODE, "forwarded-header");
    m_aOldConfig = SMPConfigProvider.setConfig (new SMPConfig (new ConfigurationSourceFunction (aConfigValues::get)));

    ServletContextPathHolder.clearContextPath ();
    ServletContextPathHolder.setCustomContextPath (CONTEXT_PATH);
  }

  @After
  public void after ()
  {
    ServletContextPathHolder.clearContextPath ();
    if (m_aOldConfig != null)
      SMPConfigProvider.setConfig (m_aOldConfig);
  }

  @NonNull
  private static SMPRestDataProvider _createDataProvider (@Nullable final String sForwardedHeaderValue)
  {
    final MockHttpServletRequest aHttpRequest = new MockHttpServletRequest ();
    aHttpRequest.setAllPaths (REQUEST_URL);
    // No servlet context is present, so the context path must be set explicitly
    aHttpRequest.setContextPath (CONTEXT_PATH);
    if (sForwardedHeaderValue != null)
      aHttpRequest.addHeader ("Forwarded", sForwardedHeaderValue);

    final IRequestWebScopeWithoutResponse aRequestScope = new RequestWebScope (aHttpRequest,
                                                                              new MockHttpServletResponse ());
    aRequestScope.initScope ();
    return new SMPRestDataProvider (aRequestScope);
  }

  @Test
  public void testForwardedHeaderWithNonDefaultPort ()
  {
    // Issue #514 - the non default port must be part of the resulting URL
    final SMPRestDataProvider aDP = _createDataProvider ("for=192.0.2.1;proto=https;host=\"example.com:8443\"");
    assertEquals ("https://example.com:8443" + CONTEXT_PATH + "/iso6523-actorid-upis%3A%3A9915%3Atest",
                  aDP.getCurrentURI ().toString ());
  }

  @Test
  public void testMalformedForwardedHeaderUsesRequestData ()
  {
    // A host containing a colon is not a valid RFC 7230 token and must be quoted - the whole
    // header value is therefore rejected by the parser
    final SMPRestDataProvider aDP = _createDataProvider ("for=192.0.2.1;proto=https;host=example.com:8443");
    assertEquals ("http://internal.host:90" + CONTEXT_PATH + "/iso6523-actorid-upis%3A%3A9915%3Atest",
                  aDP.getCurrentURI ().toString ());
  }

  @Test
  public void testForwardedHeaderWithoutPort ()
  {
    final SMPRestDataProvider aDP = _createDataProvider ("for=192.0.2.1;proto=https;host=example.com");
    assertEquals ("https://example.com" + CONTEXT_PATH + "/iso6523-actorid-upis%3A%3A9915%3Atest",
                  aDP.getCurrentURI ().toString ());
  }

  @Test
  public void testForwardedHeaderWithDefaultPort ()
  {
    final SMPRestDataProvider aDP = _createDataProvider ("for=192.0.2.1;proto=https;host=\"example.com:443\"");
    assertEquals ("https://example.com" + CONTEXT_PATH + "/iso6523-actorid-upis%3A%3A9915%3Atest",
                  aDP.getCurrentURI ().toString ());
  }

  @Test
  public void testForwardedHeaderIPv6WithPort ()
  {
    final SMPRestDataProvider aDP = _createDataProvider ("for=192.0.2.1;proto=https;host=\"[2001:db8::1]:8443\"");
    assertEquals ("https://[2001:db8::1]:8443" + CONTEXT_PATH + "/iso6523-actorid-upis%3A%3A9915%3Atest",
                  aDP.getCurrentURI ().toString ());
  }

  @Test
  public void testForwardedHeaderIPv6WithoutPort ()
  {
    final SMPRestDataProvider aDP = _createDataProvider ("for=192.0.2.1;proto=https;host=\"[2001:db8::1]\"");
    assertEquals ("https://[2001:db8::1]" + CONTEXT_PATH + "/iso6523-actorid-upis%3A%3A9915%3Atest",
                  aDP.getCurrentURI ().toString ());
  }

  @Test
  public void testNoForwardedHeaderUsesRequestData ()
  {
    final SMPRestDataProvider aDP = _createDataProvider (null);
    assertEquals ("http://internal.host:90" + CONTEXT_PATH + "/iso6523-actorid-upis%3A%3A9915%3Atest",
                  aDP.getCurrentURI ().toString ());
  }

  @Test
  public void testServiceGroupHrefWithNonDefaultPort ()
  {
    final SMPRestDataProvider aDP = _createDataProvider ("for=192.0.2.1;proto=https;host=\"example.com:8443\"");
    assertEquals ("https://example.com:8443" + CONTEXT_PATH,
                  aDP.getBaseUriBuilder ());
  }
}
