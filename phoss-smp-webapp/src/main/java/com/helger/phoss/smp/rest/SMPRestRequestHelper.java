/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
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
import javax.annotation.concurrent.Immutable;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.http.basicauth.HttpBasicAuth;
import com.helger.phoss.smp.exception.SMPUnauthorizedException;

/**
 * This class is used for retrieving the HTTP BASIC AUTH header from the HTTP
 * Authorization Header.
 *
 * @author Philip Helger
 */
@Immutable
final class SMPRestRequestHelper
{
  private SMPRestRequestHelper ()
  {}

  /**
   * Get the basic auth from the header
   *
   * @param aHttpHeaders
   *        Headers to extract from. May not be <code>null</code>.
   * @return The extracted basic auth. Never <code>null</code>.
   * @throws SMPUnauthorizedException
   *         If no BasicAuth HTTP header is present
   */
  @Nonnull
  public static BasicAuthClientCredentials getMandatoryAuth (@Nonnull final HttpHeaderMap aHttpHeaders) throws SMPUnauthorizedException
  {
    final ICommonsList <String> aHeaders = aHttpHeaders.getAllHeaderValues (CHttpHeader.AUTHORIZATION);
    if (aHeaders.isEmpty ())
      throw new SMPUnauthorizedException ("Missing required HTTP header '" + CHttpHeader.AUTHORIZATION + "' for user authentication");

    return HttpBasicAuth.getBasicAuthClientCredentials (aHeaders.getFirst ());
  }
}
