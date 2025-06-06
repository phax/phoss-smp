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
package com.helger.phoss.smp.rest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.regex.RegExHelper;
import com.helger.commons.string.StringHelper;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.http.basicauth.HttpBasicAuth;
import com.helger.phoss.smp.exception.SMPUnauthorizedException;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.photon.api.IAPIExecutor;

abstract class AbstractSMPAPIExecutor implements IAPIExecutor
{
  protected static final boolean XML_SCHEMA_VALIDATION = true;

  /**
   * Get the Bearer Token credentials from the passed HTTP header value.
   *
   * @param sAuthHeader
   *        The HTTP header value to be interpreted. May be <code>null</code>.
   * @return <code>null</code> if the passed value is not a correct HTTP Bearer
   *         header value.
   */
  @Nullable
  private static String _getBearerToken (@Nullable final String sAuthHeader)
  {
    final String sRealHeader = StringHelper.trim (sAuthHeader);
    if (StringHelper.hasNoText (sRealHeader))
      return null;

    final String [] aElements = RegExHelper.getSplitToArray (sRealHeader, "\\s+", 2);
    if (aElements.length != 2)
      return null;

    if (!aElements[0].equals ("Bearer"))
      return null;

    return aElements[1];
  }

  /**
   * Get the authorization from the HTTP headers
   *
   * @param aHttpHeaders
   *        Headers to extract from. May not be <code>null</code>.
   * @return The extracted basic auth. Never <code>null</code>.
   * @throws SMPUnauthorizedException
   *         If no BasicAuth HTTP header is present
   */
  @Nonnull
  public static SMPAPICredentials getMandatoryAuth (@Nonnull final HttpHeaderMap aHttpHeaders) throws SMPUnauthorizedException
  {
    final ICommonsList <String> aHeaders = aHttpHeaders.getAllHeaderValues (CHttpHeader.AUTHORIZATION);
    if (aHeaders.isEmpty ())
      throw new SMPUnauthorizedException ("Missing required HTTP header '" +
                                          CHttpHeader.AUTHORIZATION +
                                          "' for user authentication");

    final String sAuthHeader = aHeaders.getFirstOrNull ();

    // Check bearer token first (does not log in case of error)
    final String sBearerToken = _getBearerToken (sAuthHeader);
    if (StringHelper.hasText (sBearerToken))
      return SMPAPICredentials.createForBearerToken (sBearerToken);

    // Now try BasicAuth
    final BasicAuthClientCredentials aBasicAuth = HttpBasicAuth.getBasicAuthClientCredentials (sAuthHeader);
    if (aBasicAuth != null)
      return SMPAPICredentials.createForBasicAuth (aBasicAuth);

    throw new SMPUnauthorizedException ("The HTTP header '" +
                                        CHttpHeader.AUTHORIZATION +
                                        "' is malformed. Contains neither a Bearer Token nor Basic Auth");
  }
}
