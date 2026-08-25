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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.http.CHttpHeader;
import com.helger.http.header.HttpHeaderMap;
import com.helger.phoss.smp.exception.SMPUnauthorizedException;
import com.helger.phoss.smp.restapi.SMPAPICredentials;

/**
 * Test class for class {@link AbstractSMPAPIExecutor}.
 *
 * @author vinit-thummar
 */
public final class AbstractSMPAPIExecutorTest
{
  @Test
  public void testBearerSchemeIsCaseInsensitive () throws SMPUnauthorizedException
  {
    for (final String sScheme : new String [] { "Bearer", "bearer", "BEARER", "bEaReR" })
    {
      final HttpHeaderMap aHeaders = new HttpHeaderMap ();
      aHeaders.addHeader (CHttpHeader.AUTHORIZATION, sScheme + " test-token");

      final SMPAPICredentials aCredentials = AbstractSMPAPIExecutor.getMandatoryAuth (aHeaders);
      assertTrue (aCredentials.hasBearerToken ());
      assertEquals ("test-token", aCredentials.getBearerToken ());
    }
  }
}
