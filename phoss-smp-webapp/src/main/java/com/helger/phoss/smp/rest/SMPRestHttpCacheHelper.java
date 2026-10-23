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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.http.CHttpHeader;
import com.helger.http.cache.CacheControlBuilder;
import com.helger.mime.CMimeType;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.security.messagedigest.EMessageDigestAlgorithm;
import com.helger.security.messagedigest.MessageDigestValue;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.write.XMLWriterSettings;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Helper to apply the optional HTTP caching headers (<code>Cache-Control</code>,
 * <code>ETag</code>, <code>Vary</code>) and the conditional request handling to the responses of
 * the read-only REST API endpoints <code>GET /{ServiceGroupId}</code> and
 * <code>GET /{ServiceGroupId}/services/{DocumentTypeId}</code>.
 *
 * @author Philip Helger
 * @since 8.7.0
 */
public final class SMPRestHttpCacheHelper
{
  private static final String HTTP_IF_NONE_MATCH = "If-None-Match";

  private SMPRestHttpCacheHelper ()
  {}

  /**
   * Check if the provided <code>If-None-Match</code> header value matches the provided ETag, using
   * the weak comparison of RFC 9110 section 13.1.2.
   *
   * @param sIfNoneMatch
   *        The header value. May be <code>null</code>.
   * @param sETag
   *        The quoted ETag of the current response. May not be <code>null</code>.
   * @return <code>true</code> if the ETag matches or if the header value is <code>*</code>.
   */
  static boolean isIfNoneMatch (@Nullable final String sIfNoneMatch, @NonNull final String sETag)
  {
    final String sRealValue = StringHelper.trim (sIfNoneMatch);
    if (StringHelper.isEmpty (sRealValue))
      return false;
    if (sRealValue.equals ("*"))
      return true;

    for (final String sPart : StringHelper.getExploded (',', sRealValue))
    {
      String sCandidate = sPart.trim ();
      if (sCandidate.startsWith ("W/"))
        sCandidate = sCandidate.substring (2);
      if (sCandidate.equals (sETag))
        return true;
    }
    return false;
  }

  /**
   * Set the response content and - depending on the configuration - the HTTP caching headers. If
   * the request is a conditional request that matches the ETag, a
   * <code>304 Not Modified</code> without content is set.
   *
   * @param aRequestScope
   *        The current request scope. May not be <code>null</code>.
   * @param aUnifiedResponse
   *        The response to fill. May not be <code>null</code>.
   * @param aBytes
   *        The XML response bytes. May not be <code>null</code>.
   * @param aVaryDataProvider
   *        The data provider used to determine the <code>Vary</code> header. Only pass a non-
   *        <code>null</code> value if the response contains absolute URLs. May be
   *        <code>null</code>.
   */
  public static void applyResponse (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                    @NonNull final PhotonUnifiedResponse aUnifiedResponse,
                                    final byte @NonNull [] aBytes,
                                    @Nullable final SMPRestDataProvider aVaryDataProvider)
  {
    ValueEnforcer.notNull (aRequestScope, "RequestScope");
    ValueEnforcer.notNull (aUnifiedResponse, "UnifiedResponse");
    ValueEnforcer.notNull (aBytes, "Bytes");

    final long nMaxAgeSecs = SMPServerConfiguration.getRestCacheHttpMaxAge ().toSeconds ();
    final boolean bETag = SMPServerConfiguration.isRestCacheHttpETagEnabled ();

    if (nMaxAgeSecs > 0 || bETag)
    {
      // Replaces the "disableCaching" headers of AbstractSMPAPIExecutor
      if (nMaxAgeSecs > 0)
        aUnifiedResponse.enableCaching ((int) Math.min (nMaxAgeSecs, Integer.MAX_VALUE));
      else
      {
        // May be stored but must always be revalidated
        aUnifiedResponse.removeCaching ();
        aUnifiedResponse.setCacheControl (new CacheControlBuilder ().setNoCache (true));
      }

      if (aVaryDataProvider != null)
      {
        // The response contains absolute URLs taken from the proxy headers
        switch (aVaryDataProvider.getEffectiveServerNameMode ())
        {
          case X_FORWARDED_HEADER:
            aUnifiedResponse.addCustomResponseHeader (CHttpHeader.VARY,
                                                      "X-Forwarded-Proto, X-Forwarded-Host, X-Forwarded-Port");
            break;
          case FORWARDED_HEADER:
            aUnifiedResponse.addCustomResponseHeader (CHttpHeader.VARY, "Forwarded");
            break;
          default:
            break;
        }
      }

      if (bETag)
      {
        final String sETag = "\"" +
                             MessageDigestValue.create (aBytes, EMessageDigestAlgorithm.SHA_256)
                                               .getHexEncodedDigestString () +
                             "\"";
        // Must be called after removeCaching, because that removes the ETag as well
        aUnifiedResponse.setETagIfApplicable (sETag);

        if (isIfNoneMatch (aRequestScope.headers ().getFirstHeaderValue (HTTP_IF_NONE_MATCH), sETag))
        {
          // Keep the ETag and Cache-Control headers (RFC 9110 section 15.4.5)
          aUnifiedResponse.setStatus (HttpServletResponse.SC_NOT_MODIFIED);
          return;
        }
      }
    }

    aUnifiedResponse.setContent (aBytes)
                    .setMimeType (CMimeType.TEXT_XML)
                    .setCharset (XMLWriterSettings.DEFAULT_XML_CHARSET_OBJ);
  }
}
