/**
 * Copyright (C) 2014-2020 Philip Helger and contributors
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
package com.helger.phoss.smp.ui.secure;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.http.EHttpMethod;
import com.helger.commons.http.HttpHeaderMap;
import com.helger.commons.id.IHasID;
import com.helger.commons.name.IHasDisplayName;
import com.helger.commons.string.StringHelper;
import com.helger.commons.timing.StopWatch;
import com.helger.commons.url.SimpleURL;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.html.forms.HCTextArea;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.httpclient.HttpClientFactory;
import com.helger.httpclient.HttpClientHelper;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.response.ResponseHandlerHttpEntity;
import com.helger.pd.client.PDHttpClientFactory;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.button.BootstrapSubmitButton;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapTechnicalUI;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.html.select.HCExtSelect;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTablesLengthMenu;
import com.helger.photon.uictrls.datatables.column.DTCol;

public class PageSecureHttpClient extends AbstractSMPWebPage
{
  public static interface IHttpClientMetaProvider
  {
    /**
     * @param sTargetURI
     *        The target URL to be accessed. May neither be <code>null</code>
     *        nor empty.
     * @return The client factory to use. May not be <code>null</code>.
     */
    @Nonnull
    HttpClientFactory getHttpClientFactory (@Nonnull @Nonempty String sTargetURI);
  }

  public static interface IHttpClientConfig extends IHasID <String>, IHasDisplayName, IHttpClientMetaProvider
  {
    /* empty */
  }

  public static class HttpClientConfig implements IHttpClientConfig
  {
    private final String m_sID;
    private final String m_sDisplayName;
    private final IHttpClientMetaProvider m_aHCMP;

    public HttpClientConfig (@Nonnull @Nonempty final String sID,
                             @Nonnull @Nonempty final String sDisplayName,
                             @Nonnull final IHttpClientMetaProvider aHCMP)
    {
      ValueEnforcer.notEmpty (sID, "ID");
      ValueEnforcer.notEmpty (sDisplayName, "DisplayName");
      ValueEnforcer.notNull (aHCMP, "HCMP");
      m_sID = sID;
      m_sDisplayName = sDisplayName;
      m_aHCMP = aHCMP;
    }

    @Nonnull
    @Nonempty
    public String getID ()
    {
      return m_sID;
    }

    @Nonnull
    @Nonempty
    public String getDisplayName ()
    {
      return m_sDisplayName;
    }

    @Nonnull
    public HttpClientFactory getHttpClientFactory (@Nonnull @Nonempty final String sTargetURI)
    {
      return m_aHCMP.getHttpClientFactory (sTargetURI);
    }
  }

  @NotThreadSafe
  public static class HttpClientConfigRegistry
  {
    private static final ICommonsMap <String, IHttpClientConfig> s_aMap = new CommonsHashMap <> ();

    private HttpClientConfigRegistry ()
    {}

    public static void register (@Nonnull final IHttpClientConfig aHCC)
    {
      ValueEnforcer.notNull (aHCC, "HCC");
      final String sID = aHCC.getID ();
      if (s_aMap.containsKey (sID))
        throw new IllegalArgumentException ("Another configuration with ID '" + sID + "' is already registered");
      s_aMap.put (sID, aHCC);
    }

    @Nullable
    public static IHttpClientConfig getFromID (@Nullable final String sID)
    {
      if (StringHelper.hasNoText (sID))
        return null;
      return s_aMap.get (sID);
    }

    @Nonnull
    public static Iterable <IHttpClientConfig> iterate ()
    {
      return s_aMap.values ();
    }

    static
    {
      register (new HttpClientConfig ("systemdefault", "System default settings", x -> new HttpClientFactory ()));
    }
  }

  static
  {
    HttpClientConfigRegistry.register (new HttpClientConfig ("directoryclient",
                                                             "Directory client settings",
                                                             x -> new PDHttpClientFactory (x.startsWith ("https:"))));
  }

  private static final class DebugResponseHandler implements ResponseHandler <String>
  {
    private final Charset m_aDefaultCharset;
    private StatusLine m_aUsedStatusLine;
    private Charset m_aUsedCharset;
    private final HttpHeaderMap m_aUsedHeaders = new HttpHeaderMap ();

    public DebugResponseHandler (@Nonnull final Charset aDefaultCharset)
    {
      m_aDefaultCharset = aDefaultCharset;
    }

    @Nullable
    public String handleResponse (@Nonnull final HttpResponse aHttpResponse) throws IOException
    {
      // Convert to entity
      final HttpEntity aEntity = ResponseHandlerHttpEntity.INSTANCE.handleResponse (aHttpResponse);
      if (aEntity == null)
        return null;

      final Charset aCharset;
      final ContentType aContentType = ContentType.get (aEntity);
      if (aContentType == null)
        aCharset = m_aDefaultCharset;
      else
      {
        // Get the charset from the content type or the default charset
        aCharset = HttpClientHelper.getCharset (aContentType, m_aDefaultCharset);
      }

      m_aUsedStatusLine = aHttpResponse.getStatusLine ();
      m_aUsedCharset = aCharset;
      m_aUsedHeaders.removeAll ();
      for (final Header aHeader : aHttpResponse.getAllHeaders ())
        m_aUsedHeaders.addHeader (aHeader.getName (), aHeader.getValue ());

      return EntityUtils.toString (aEntity, aCharset);
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureHttpClient.class);
  private static final String FIELD_CONFIG = "config";
  private static final String FIELD_HTTP_METHOD = "http_method";
  private static final String FIELD_URI = "uri";

  public PageSecureHttpClient (@Nonnull @Nonempty final String sID)
  {
    super (sID, "http(s) client");
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    aNodeList.addChild (info ("This page allows to perform arbitrary http(s) queries to test network connectivity problems."));

    final FormErrorList aFormErrors = new FormErrorList ();
    if (aWPEC.hasAction (CPageParam.ACTION_PERFORM))
    {
      final String sConfigID = aWPEC.params ().getAsStringTrimmed (FIELD_CONFIG);
      final IHttpClientConfig aConfig = HttpClientConfigRegistry.getFromID (sConfigID);
      final String sHttpMethod = aWPEC.params ().getAsStringTrimmed (FIELD_HTTP_METHOD);
      final EHttpMethod eHttpMethod = EHttpMethod.getFromNameOrNull (sHttpMethod);
      final String sURI = aWPEC.params ().getAsStringTrimmed (FIELD_URI);

      if (StringHelper.hasNoText (sConfigID))
        aFormErrors.addFieldError (FIELD_CONFIG, "A configuration must be selected.");
      else
        if (aConfig == null)
          aFormErrors.addFieldError (FIELD_CONFIG, "Please select a valid configuration.");

      if (StringHelper.hasNoText (sHttpMethod))
        aFormErrors.addFieldError (FIELD_HTTP_METHOD, "A HTTP method must be selected.");
      else
        if (eHttpMethod == null)
          aFormErrors.addFieldError (FIELD_HTTP_METHOD, "Please select a valid HTTP method.");

      if (StringHelper.hasNoText (sURI))
        aFormErrors.addFieldError (FIELD_URI, "A URI must be provided.");
      else
        if (!sURI.startsWith ("http://") && !sURI.startsWith ("https://"))
          aFormErrors.addFieldError (FIELD_URI, "The URI must start with 'http://' or 'https://'");

      if (aFormErrors.isEmpty ())
      {
        String sResultContent;
        boolean bSuccess = false;

        LOGGER.info ("http client query '" + sURI + "' using configuration " + aConfig.getID ());

        final StopWatch aSW = StopWatch.createdStarted ();
        final HttpClientFactory aHCP = aConfig.getHttpClientFactory (sURI);
        final DebugResponseHandler aResponseHdl = new DebugResponseHandler (StandardCharsets.UTF_8);
        try (final HttpClientManager aHCM = new HttpClientManager (aHCP))
        {
          // Create depending on the method
          final HttpRequestBase aReq = HttpClientHelper.createRequest (eHttpMethod, new SimpleURL (sURI));
          sResultContent = aHCM.execute (aReq, aResponseHdl);
          bSuccess = true;
        }
        catch (final IOException ex)
        {
          sResultContent = BootstrapTechnicalUI.getTechnicalDetailsString (ex, aDisplayLocale);
        }
        aSW.stop ();

        aNodeList.addChild (div ("Output of querying ").addChild (code (sURI))
                                                       .addChild (" using ")
                                                       .addChild (em (aConfig.getDisplayName ()))
                                                       .addChild (": ")
                                                       .addChild (bSuccess ? badgeSuccess ("success")
                                                                           : badgeDanger ("error")));
        aNodeList.addChild (div ("Querying took " + aSW.getMillis () + " milliseconds"));
        if (aResponseHdl.m_aUsedStatusLine != null)
        {
          // toString of ProtocolVersion is fine
          aNodeList.addChild (div ("Response protocol version: ").addChild (code (String.valueOf (aResponseHdl.m_aUsedStatusLine.getProtocolVersion ()))));
          aNodeList.addChild (div ("Response status code: ").addChild (code (Integer.toString (aResponseHdl.m_aUsedStatusLine.getStatusCode ()))));
          aNodeList.addChild (div ("Response reason phrase: ").addChild (code (aResponseHdl.m_aUsedStatusLine.getReasonPhrase ())));
        }
        if (aResponseHdl.m_aUsedCharset != null)
          aNodeList.addChild (div ("Response charset used: ").addChild (code (aResponseHdl.m_aUsedCharset.name ())));
        if (aResponseHdl.m_aUsedHeaders.isNotEmpty ())
        {
          aNodeList.addChild (div ("Response HTTP headers:"));
          final HCTable aTable = new HCTable (new DTCol ("Name").setInitialSorting (ESortOrder.ASCENDING),
                                              new DTCol ("Value")).setID ("httpresponseheaders");
          aResponseHdl.m_aUsedHeaders.forEachSingleHeader ( (n, v) -> aTable.addBodyRow ().addCells (n, v), false);
          final BootstrapDataTables aDT = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
          aDT.setLengthMenu (DataTablesLengthMenu.INSTANCE_ALL);
          aDT.setPaging (false);
          aDT.setInfo (false);
          aNodeList.addChild (aTable).addChild (aDT);
        }
        aNodeList.addChild (new HCTextArea ("responsepayload").setRows (Math.min (10,
                                                                                  1 +
                                                                                      StringHelper.getCharCount (sResultContent,
                                                                                                                 '\n')))
                                                              .setValue (sResultContent)
                                                              .addClass (CBootstrapCSS.FORM_CONTROL)
                                                              .addClass (CBootstrapCSS.TEXT_MONOSPACE)
                                                              .addClass (CBootstrapCSS.MB_2));
      }
    }

    final BootstrapForm aForm = aNodeList.addAndReturnChild (new BootstrapForm (aWPEC));
    aForm.setLeft (2);

    {
      final HCExtSelect aSelect = new HCExtSelect (new RequestField (FIELD_CONFIG));
      aSelect.addOptionPleaseSelect (aDisplayLocale);
      for (final IHttpClientConfig aHCC : HttpClientConfigRegistry.iterate ())
        aSelect.addOption (aHCC.getID (), aHCC.getDisplayName ());
      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Configuration to use")
                                                   .setCtrl (aSelect)
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_CONFIG)));
    }
    {
      final HCExtSelect aSelect = new HCExtSelect (new RequestField (FIELD_HTTP_METHOD, EHttpMethod.GET.getName ()));
      aSelect.addOptionPleaseSelect (aDisplayLocale);
      for (final EHttpMethod e : EHttpMethod.values ())
        if (e != EHttpMethod.CONNECT && e != EHttpMethod.PATCH)
          aSelect.addOption (e.getName ());
      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("HTTP method")
                                                   .setCtrl (aSelect)
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_HTTP_METHOD)));
    }
    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("URI to query")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_URI)))
                                                 .setHelpText (new HCTextNode ("The URI to query. Must start with "),
                                                               code ("http://"),
                                                               new HCTextNode (" or "),
                                                               code ("https://"),
                                                               new HCTextNode ("."))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_URI)));
    aForm.addChild (new HCHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM));
    aForm.addChild (new BootstrapSubmitButton ().addChild ("Query now"));
  }
}
