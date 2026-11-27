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

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.datetime.domain.IHasLastModificationOffsetDateTime;
import com.helger.datetime.web.PDTWebDateHelper;
import com.helger.http.CHttpHeader;
import com.helger.http.cache.CacheControlBuilder;
import com.helger.mime.CMimeType;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.restapi.SMPRestLastModification;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.security.messagedigest.EMessageDigestAlgorithm;
import com.helger.security.messagedigest.MessageDigestValue;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.write.XMLWriterSettings;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Helper to apply the optional HTTP caching headers (<code>Cache-Control</code>,
 * <code>ETag</code>, <code>Last-Modified</code>, <code>Vary</code>) and the conditional request
 * handling to the responses of the read-only REST API endpoints <code>GET /{ServiceGroupId}</code>
 * and <code>GET /{ServiceGroupId}/services/{DocumentTypeId}</code>.
 *
 * @author Philip Helger
 * @since 8.7.0
 */
public final class SMPRestHttpCacheHelper
{
  private static final String HTTP_IF_NONE_MATCH = "If-None-Match";
  private static final String HTTP_IF_MODIFIED_SINCE = "If-Modified-Since";

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
   * Check if the provided <code>If-Modified-Since</code> header value is equal to or later than the
   * provided last modification.
   *
   * @param sIfModifiedSince
   *        The header value. May be <code>null</code>.
   * @param aLastModDT
   *        The last modification as sent in the <code>Last-Modified</code> header, so with full
   *        seconds. May not be <code>null</code>.
   * @return <code>true</code> if the header value is a valid HTTP date and the resource was not
   *         modified since then, <code>false</code> otherwise.
   */
  static boolean isNotModifiedSince (@Nullable final String sIfModifiedSince, @NonNull final OffsetDateTime aLastModDT)
  {
    final String sRealValue = StringHelper.trim (sIfModifiedSince);
    if (StringHelper.isEmpty (sRealValue))
      return false;

    final ZonedDateTime aIfModifiedSince;
    try
    {
      // Strictly the IMF-fixdate of RFC 9110 section 5.6.7
      aIfModifiedSince = ZonedDateTime.parse (sRealValue, DateTimeFormatter.RFC_1123_DATE_TIME);
    }
    catch (final DateTimeParseException ex)
    {
      // An invalid date is ignored (RFC 9110 section 13.1.3)
      return false;
    }

    return !aLastModDT.toInstant ().isAfter (aIfModifiedSince.toInstant ());
  }

  /**
   * Get the last modification of a participant that is relevant for the HTTP header
   * <code>Last-Modified</code>. This must be called before the response is created, so that the
   * response is never older than the last modification.
   *
   * @param aParticipantID
   *        The participant of the response. May be <code>null</code>.
   * @return <code>null</code> if the <code>Last-Modified</code> header is disabled in the
   *         configuration, if the participant ID is <code>null</code> or if the participant was not
   *         modified since the backend maintains the last modification.
   */
  @Nullable
  public static OffsetDateTime getParticipantLastModificationDateTime (@Nullable final IParticipantIdentifier aParticipantID)
  {
    if (aParticipantID == null || !SMPServerConfiguration.isRestCacheHttpLastModifiedEnabled ())
      return null;
    final IHasLastModificationOffsetDateTime aLastMod = SMPMetaManager.getServiceGroupMgr ()
                                                                .getSMPServiceGroupLastModification (aParticipantID);
    return aLastMod == null ? null : aLastMod.getLastModificationDateTime ();
  }

  /**
   * Get the value of the <code>Last-Modified</code> header to be sent.
   *
   * @param aParticipantLastModDT
   *        The last modification of the participant as returned by
   *        {@link #getParticipantLastModificationDateTime(IParticipantIdentifier)}. May be
   *        <code>null</code>.
   * @param aNow
   *        The current date time. May not be <code>null</code>.
   * @return The effective last modification with full seconds, or <code>null</code> if it lies in
   *         the current second. HTTP dates have a resolution of one second, so another change in the
   *         same second could otherwise never be detected by a client.
   */
  @Nullable
  static OffsetDateTime getLastModifiedHeaderValue (@Nullable final OffsetDateTime aParticipantLastModDT,
                                                   @NonNull final OffsetDateTime aNow)
  {
    final OffsetDateTime ret = SMPRestLastModification.getEffectiveLastModificationDateTime (aParticipantLastModDT)
                                                     .truncatedTo (ChronoUnit.SECONDS);
    return ret.isBefore (aNow.truncatedTo (ChronoUnit.SECONDS)) ? ret : null;
  }

  /**
   * @return <code>true</code> if at least one of the HTTP caching headers is enabled in the
   *         configuration. Only then <code>HEAD</code> requests are answered.
   */
  public static boolean isAnyHttpCachingEnabled ()
  {
    return SMPServerConfiguration.getRestCacheHttpMaxAge ().toSeconds () > 0 ||
           SMPServerConfiguration.isRestCacheHttpETagEnabled () ||
           SMPServerConfiguration.isRestCacheHttpLastModifiedEnabled ();
  }

  /**
   * Set the HTTP caching headers depending on the configuration and evaluate the conditional
   * request headers.
   *
   * @param aRequestScope
   *        The current request scope. May not be <code>null</code>.
   * @param aUnifiedResponse
   *        The response to fill. May not be <code>null</code>.
   * @param aBytes
   *        The XML response bytes, used for the ETag. May be <code>null</code> for a
   *        <code>HEAD</code> request, in which case no ETag is sent.
   * @param aVaryDataProvider
   *        The data provider used to determine the <code>Vary</code> header. May be
   *        <code>null</code>.
   * @param aParticipantLastModDT
   *        The last modification of the participant. May be <code>null</code>.
   * @return <code>true</code> if the response is not modified and <code>304 Not Modified</code>
   *         was set, <code>false</code> otherwise.
   */
  private static boolean _applyCachingHeaders (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                               @NonNull final PhotonUnifiedResponse aUnifiedResponse,
                                               final byte @Nullable [] aBytes,
                                               @Nullable final SMPRestDataProvider aVaryDataProvider,
                                               @Nullable final OffsetDateTime aParticipantLastModDT)
  {
    final long nMaxAgeSecs = SMPServerConfiguration.getRestCacheHttpMaxAge ().toSeconds ();
    final boolean bETag = SMPServerConfiguration.isRestCacheHttpETagEnabled ();
    final boolean bLastModified = SMPServerConfiguration.isRestCacheHttpLastModifiedEnabled ();

    if (nMaxAgeSecs <= 0 && !bETag && !bLastModified)
      return false;

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

    final String sIfNoneMatch = aRequestScope.headers ().getFirstHeaderValue (HTTP_IF_NONE_MATCH);
    boolean bNotModified = false;

    // The ETag is determined while generating the content, so it is omitted for HEAD (RFC 9110
    // section 9.3.2)
    if (bETag && aBytes != null)
    {
      final String sETag = "\"" +
                           MessageDigestValue.create (aBytes, EMessageDigestAlgorithm.SHA_256)
                                             .getHexEncodedDigestString () +
                           "\"";
      // Must be called after removeCaching, because that removes the ETag as well
      aUnifiedResponse.setETagIfApplicable (sETag);

      if (isIfNoneMatch (sIfNoneMatch, sETag))
        bNotModified = true;
    }

    if (bLastModified)
    {
      final OffsetDateTime aLastModDT = getLastModifiedHeaderValue (aParticipantLastModDT,
                                                                   SMPMetaManager.getTimestampMgr ().getCurrentDateTimeUTC ());
      if (aLastModDT != null)
      {
        // Must be called after removeCaching, because that removes the Last-Modified as well
        aUnifiedResponse.setLastModified (aLastModDT);

        // If-Modified-Since is ignored if If-None-Match is present (RFC 9110 section 13.2.2)
        if (sIfNoneMatch == null &&
            isNotModifiedSince (aRequestScope.headers ().getFirstHeaderValue (HTTP_IF_MODIFIED_SINCE), aLastModDT))
          bNotModified = true;
      }
    }

    if (bNotModified)
    {
      // Keep the validator and Cache-Control headers (RFC 9110 section 15.4.5)
      aUnifiedResponse.setStatus (HttpServletResponse.SC_NOT_MODIFIED);
    }
    return bNotModified;
  }

  /**
   * Set the response content and - depending on the configuration - the HTTP caching headers. If
   * the request is a conditional request that matches the ETag or the last modification, a
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
   * @param aParticipantLastModDT
   *        The last modification of the participant as returned by
   *        {@link #getParticipantLastModificationDateTime(IParticipantIdentifier)} before the
   *        response bytes were created. May be <code>null</code>.
   */
  public static void applyResponse (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                    @NonNull final PhotonUnifiedResponse aUnifiedResponse,
                                    final byte @NonNull [] aBytes,
                                    @Nullable final SMPRestDataProvider aVaryDataProvider,
                                    @Nullable final OffsetDateTime aParticipantLastModDT)
  {
    ValueEnforcer.notNull (aRequestScope, "RequestScope");
    ValueEnforcer.notNull (aUnifiedResponse, "UnifiedResponse");
    ValueEnforcer.notNull (aBytes, "Bytes");

    if (_applyCachingHeaders (aRequestScope, aUnifiedResponse, aBytes, aVaryDataProvider, aParticipantLastModDT))
      return;

    aUnifiedResponse.setContent (aBytes)
                    .setMimeType (CMimeType.TEXT_XML)
                    .setCharset (XMLWriterSettings.DEFAULT_XML_CHARSET_OBJ);
  }

  /**
   * Set the response of a <code>HEAD</code> request, for which the response content was not
   * created. The same caching headers as for <code>GET</code> are set, except for the
   * <code>ETag</code> and the <code>Content-Length</code>, because they depend on the content
   * (RFC 9110 section 9.3.2).
   *
   * @param aRequestScope
   *        The current request scope. May not be <code>null</code>.
   * @param aUnifiedResponse
   *        The response to fill. May not be <code>null</code>.
   * @param aVaryDataProvider
   *        The data provider used to determine the <code>Vary</code> header. Only pass a non-
   *        <code>null</code> value if the <code>GET</code> response contains absolute URLs. May be
   *        <code>null</code>.
   * @param aParticipantLastModDT
   *        The last modification of the participant. May be <code>null</code>.
   * @since 8.9.0
   */
  public static void applyHeadResponse (@NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                                        @NonNull final PhotonUnifiedResponse aUnifiedResponse,
                                        @Nullable final SMPRestDataProvider aVaryDataProvider,
                                        @Nullable final OffsetDateTime aParticipantLastModDT)
  {
    ValueEnforcer.notNull (aRequestScope, "RequestScope");
    ValueEnforcer.notNull (aUnifiedResponse, "UnifiedResponse");

    if (_applyCachingHeaders (aRequestScope, aUnifiedResponse, null, aVaryDataProvider, aParticipantLastModDT))
      return;

    // No content, so the MIME type must be set as a header
    aUnifiedResponse.addCustomResponseHeader (CHttpHeader.CONTENT_TYPE,
                                              CMimeType.TEXT_XML.getAsString () +
                                                                   "; charset=" +
                                                                   XMLWriterSettings.DEFAULT_XML_CHARSET_OBJ.name ());
    aUnifiedResponse.setStatus (HttpServletResponse.SC_OK);
  }
}
