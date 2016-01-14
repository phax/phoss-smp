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
package com.helger.peppol.smpserver.rest;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.ws.rs.core.HttpHeaders;

import com.helger.commons.collection.CollectionHelper;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;
import com.helger.web.http.basicauth.BasicAuthClientCredentials;
import com.helger.web.http.basicauth.HTTPBasicAuth;

/**
 * This class is used for retrieving the HTTP BASIC AUTH header from the HTTP
 * Authorization Header.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Immutable
final class RestRequestHelper
{
  private RestRequestHelper ()
  {}

  @Nonnull
  public static BasicAuthClientCredentials getAuth (@Nonnull final HttpHeaders aHttpHeaders) throws SMPUnauthorizedException
  {
    final List <String> aHeaders = aHttpHeaders.getRequestHeader (HttpHeaders.AUTHORIZATION);
    if (CollectionHelper.isEmpty (aHeaders))
      throw new SMPUnauthorizedException ("Missing required HTTP header '" +
                                          HttpHeaders.AUTHORIZATION +
                                          "' for user authentication");

    final String sAuthHeader = CollectionHelper.getFirstElement (aHeaders);
    return HTTPBasicAuth.getBasicAuthClientCredentials (sAuthHeader);
  }
}
