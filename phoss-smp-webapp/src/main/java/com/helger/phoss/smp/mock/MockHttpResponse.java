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
package com.helger.phoss.smp.mock;

import java.nio.charset.Charset;

import org.apache.hc.core5.http.ContentType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.array.ArrayHelper;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.httpclient.HttpClientHelper;

/**
 * The result of a single HTTP request performed with {@link MockHttpClient}. It contains the HTTP
 * status code, the response content type and the response body. Contrary to most response handlers
 * this class is also filled for unsuccessful responses, because the unit tests need to check the
 * error status codes as well.
 *
 * @author Philip Helger
 */
public class MockHttpResponse
{
  private final int m_nStatusCode;
  private final ContentType m_aContentType;
  private final byte [] m_aBody;

  public MockHttpResponse (final int nStatusCode,
                           @Nullable final ContentType aContentType,
                           final byte @Nullable [] aBody)
  {
    m_nStatusCode = nStatusCode;
    m_aContentType = aContentType;
    m_aBody = aBody;
  }

  /**
   * @return The HTTP status code of the response.
   */
  public final int getStatusCode ()
  {
    return m_nStatusCode;
  }

  /**
   * @return The content type of the response. May be <code>null</code> if the response has no body.
   */
  @Nullable
  public final ContentType getContentType ()
  {
    return m_aContentType;
  }

  /**
   * @return The MIME type of the response, without any parameter like the charset. May be
   *         <code>null</code> if the response has no body.
   */
  @Nullable
  public final String getMimeType ()
  {
    return m_aContentType == null ? null : m_aContentType.getMimeType ();
  }

  /**
   * @return The charset of the response. Falls back to the HTTP default charset if the response
   *         contains none. Never <code>null</code>.
   */
  @NonNull
  public final Charset getCharset ()
  {
    return HttpClientHelper.getCharset (m_aContentType == null ? ContentType.DEFAULT_TEXT : m_aContentType);
  }

  /**
   * @return The response body bytes. May be <code>null</code> if the response has no body.
   */
  @Nullable
  @ReturnsMutableObject
  public final byte [] directGetBody ()
  {
    return m_aBody;
  }

  /**
   * @return <code>true</code> if a non-empty response body is present, <code>false</code>
   *         otherwise.
   */
  public final boolean hasBody ()
  {
    return ArrayHelper.isNotEmpty (m_aBody);
  }

  /**
   * @return The response body as a String in the response charset. May be <code>null</code> if the
   *         response has no body.
   * @see #getCharset()
   */
  @Nullable
  public final String getBodyAsString ()
  {
    return m_aBody == null ? null : new String (m_aBody, getCharset ());
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (this).append ("StatusCode", m_nStatusCode)
                                       .append ("ContentType", m_aContentType)
                                       .append ("Body", getBodyAsString ())
                                       .getToString ();
  }
}
