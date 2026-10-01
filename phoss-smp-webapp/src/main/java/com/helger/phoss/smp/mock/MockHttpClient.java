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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.FileEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.http.CHttpHeader;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.httpclient.HttpClientHelper;
import com.helger.httpclient.HttpClientManager;
import com.helger.jaxb.IJAXBWriter;
import com.helger.mime.CMimeType;

/**
 * A simple HTTP client to invoke the REST API of the SMP server started by
 * {@link SMPServerRESTTestRule}. It uses the same Apache HttpClient based stack as the SMP client
 * of peppol-commons does.
 *
 * @author Philip Helger
 */
public class MockHttpClient implements AutoCloseable
{
  private static final class ResponseHandler implements HttpClientResponseHandler <MockHttpResponse>
  {
    public MockHttpResponse handleResponse (@NonNull final ClassicHttpResponse aResponse) throws IOException
    {
      final HttpEntity aEntity = aResponse.getEntity ();
      final ContentType aContentType = HttpClientHelper.getContentType (aEntity);
      final byte [] aBody = aEntity == null ? null : EntityUtils.toByteArray (aEntity);
      return new MockHttpResponse (aResponse.getCode (), aContentType, aBody);
    }
  }

  // The default text/xml content type uses iso-8859-1!
  public static final ContentType CONTENT_TYPE_TEXT_XML = ContentType.create (CMimeType.TEXT_XML.getAsString (),
                                                                              StandardCharsets.UTF_8);

  private final String m_sBaseURL;
  private final HttpClientManager m_aHttpClientMgr;

  public MockHttpClient (@NonNull @Nonempty final String sBaseURL)
  {
    ValueEnforcer.notEmpty (sBaseURL, "BaseURL");
    m_sBaseURL = sBaseURL;
    m_aHttpClientMgr = new HttpClientManager ();
  }

  @NonNull
  @Nonempty
  private String _getURL (@Nullable final String sPath)
  {
    if (StringHelper.isEmpty (sPath))
      return m_sBaseURL;
    return m_sBaseURL + (sPath.startsWith ("/") ? "" : "/") + sPath;
  }

  @NonNull
  private MockHttpResponse _execute (@NonNull final HttpUriRequestBase aRequest, @Nullable final String sAuthorization)
  {
    if (sAuthorization != null)
      aRequest.addHeader (CHttpHeader.AUTHORIZATION, sAuthorization);

    try
    {
      return m_aHttpClientMgr.execute (aRequest, new ResponseHandler ());
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException ("Failed to execute " + aRequest, ex);
    }
  }

  public void close ()
  {
    m_aHttpClientMgr.close ();
  }

  /**
   * @return The base URL of all requests, as provided in the constructor. Neither <code>null</code>
   *         nor empty.
   */
  @NonNull
  @Nonempty
  public final String getBaseURL ()
  {
    return m_sBaseURL;
  }

  @NonNull
  public MockHttpResponse get (@Nullable final String sPath)
  {
    return get (sPath, (String) null);
  }

  @NonNull
  public MockHttpResponse get (@Nullable final String sPath, @Nullable final String sAuthorization)
  {
    return _execute (new HttpGet (_getURL (sPath)), sAuthorization);
  }

  @NonNull
  public MockHttpResponse get (@Nullable final String sPath, @NonNull final BasicAuthClientCredentials aCredentials)
  {
    return get (sPath, aCredentials.getRequestValue ());
  }

  @NonNull
  public MockHttpResponse put (@Nullable final String sPath,
                               @Nullable final String sAuthorization,
                               @NonNull final HttpEntity aEntity)
  {
    ValueEnforcer.notNull (aEntity, "Entity");

    final HttpPut aRequest = new HttpPut (_getURL (sPath));
    aRequest.setEntity (aEntity);
    return _execute (aRequest, sAuthorization);
  }

  @NonNull
  public MockHttpResponse put (@Nullable final String sPath,
                               @NonNull final BasicAuthClientCredentials aCredentials,
                               @NonNull final HttpEntity aEntity)
  {
    return put (sPath, aCredentials.getRequestValue (), aEntity);
  }

  @NonNull
  public MockHttpResponse delete (@Nullable final String sPath, @Nullable final String sAuthorization)
  {
    return _execute (new HttpDelete (_getURL (sPath)), sAuthorization);
  }

  @NonNull
  public MockHttpResponse delete (@Nullable final String sPath, @NonNull final BasicAuthClientCredentials aCredentials)
  {
    return delete (sPath, aCredentials.getRequestValue ());
  }

  /**
   * Create an XML entity by marshalling the provided object.
   *
   * @param aWriter
   *        The JAXB writer to be used. May not be <code>null</code>.
   * @param aObj
   *        The object to be marshalled. May not be <code>null</code>.
   * @return The created entity. Never <code>null</code>.
   * @param <T>
   *        The type of the object to be marshalled
   */
  @NonNull
  public static <T> HttpEntity createXMLEntity (@NonNull final IJAXBWriter <T> aWriter, @NonNull final T aObj)
  {
    ValueEnforcer.notNull (aWriter, "Writer");
    ValueEnforcer.notNull (aObj, "Obj");

    final String sXML = aWriter.getAsString (aObj);
    if (sXML == null)
      throw new IllegalArgumentException ("Failed to serialize " + aObj);
    return new StringEntity (sXML, CONTENT_TYPE_TEXT_XML);
  }

  /**
   * Create an XML entity from the provided XML string.
   *
   * @param sXML
   *        The XML to be sent. May not be <code>null</code>.
   * @return The created entity. Never <code>null</code>.
   */
  @NonNull
  public static HttpEntity createXMLEntity (@NonNull final String sXML)
  {
    ValueEnforcer.notNull (sXML, "XML");

    return new StringEntity (sXML, CONTENT_TYPE_TEXT_XML);
  }

  /**
   * Create an XML entity from the content of the provided file.
   *
   * @param aFile
   *        The file to be sent. May not be <code>null</code>.
   * @return The created entity. Never <code>null</code>.
   */
  @NonNull
  public static HttpEntity createXMLEntity (@NonNull final File aFile)
  {
    ValueEnforcer.notNull (aFile, "File");

    return new FileEntity (aFile, CONTENT_TYPE_TEXT_XML);
  }
}
