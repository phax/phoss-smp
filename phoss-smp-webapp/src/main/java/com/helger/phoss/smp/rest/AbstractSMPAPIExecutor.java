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

import java.util.Map;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.cache.regex.RegExHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.http.CHttpHeader;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.http.basicauth.HttpBasicAuth;
import com.helger.http.header.HttpHeaderMap;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.phoss.smp.exception.SMPUnauthorizedException;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.api.IAPIExecutor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

abstract class AbstractSMPAPIExecutor implements IAPIExecutor
{
  protected static final boolean XML_SCHEMA_VALIDATION = true;

  /**
   * Get the Bearer Token credentials from the passed HTTP header value.
   *
   * @param sAuthHeader
   *        The HTTP header value to be interpreted. May be <code>null</code>.
   * @return <code>null</code> if the passed value is not a correct HTTP Bearer header value.
   */
  @Nullable
  private static String _getBearerToken (@Nullable final String sAuthHeader)
  {
    final String sRealHeader = StringHelper.trim (sAuthHeader);
    if (StringHelper.isEmpty (sRealHeader))
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
    if (StringHelper.isNotEmpty (sBearerToken))
      return SMPAPICredentials.createForBearerToken (sBearerToken);

    // Now try BasicAuth
    final BasicAuthClientCredentials aBasicAuth = HttpBasicAuth.getBasicAuthClientCredentials (sAuthHeader);
    if (aBasicAuth != null)
      return SMPAPICredentials.createForBasicAuth (aBasicAuth);

    throw new SMPUnauthorizedException ("The HTTP header '" +
                                        CHttpHeader.AUTHORIZATION +
                                        "' is malformed. Contains neither a Bearer Token nor Basic Auth");
  }

  protected abstract void invokeAPI (@Nonnull IAPIDescriptor aAPIDescriptor,
                                     @Nonnull @Nonempty String sPath,
                                     @Nonnull Map <String, String> aPathVariables,
                                     @Nonnull IRequestWebScopeWithoutResponse aRequestScope,
                                     @Nonnull PhotonUnifiedResponse aUnifiedResponse) throws Exception;

  public final void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                               @Nonnull @Nonempty final String sPath,
                               @Nonnull final Map <String, String> aPathVariables,
                               @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                               @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final PhotonUnifiedResponse aPUR = (PhotonUnifiedResponse) aUnifiedResponse;

    // JSON responses should always be formatted
    aPUR.setJsonWriterSettings (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED);

    // Disable caching by default
    aPUR.disableCaching ();

    invokeAPI (aAPIDescriptor, sPath, aPathVariables, aRequestScope, aPUR);
  }
}
