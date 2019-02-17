/**
 * Copyright (C) 2014-2019 Philip Helger and contributors
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
package com.helger.peppol.smpserver.rest2;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.http.EHttpMethod;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.state.EContinue;
import com.helger.photon.core.api.APIDescriptor;
import com.helger.photon.core.api.APIPath;
import com.helger.photon.core.api.GlobalAPIInvoker;
import com.helger.photon.core.api.IAPIExceptionMapper;
import com.helger.photon.core.api.InvokableAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xservlet.AbstractXFilterUnifiedResponse;

/**
 * This is the SMP REST filter that MUST be implemented as a filter on "/*"
 *
 * @author Philip Helger
 */
public class Rest2Filter extends AbstractXFilterUnifiedResponse
{
  public static final String PARAM_SERVICE_GROUP_ID = "ServiceGroupId";
  public static final String PARAM_USER_ID = "UserId";
  public static final String PARAM_DOCUMENT_TYPE_ID = "DocumentTypeId";

  private static final Logger LOGGER = LoggerFactory.getLogger (Rest2Filter.class);

  public Rest2Filter ()
  {
    final IAPIExceptionMapper aExceptionMapper = new Rest2ExceptionMapper ();
    final GlobalAPIInvoker aInvoker = GlobalAPIInvoker.getInstance ();

    // BusinessCard
    {
      final APIDescriptor aGetBusinessCard = new APIDescriptor (APIPath.get ("/businesscard/{" +
                                                                             PARAM_SERVICE_GROUP_ID +
                                                                             "}"),
                                                                new APIExecutorBusinessCardGet ());
      aGetBusinessCard.setExceptionMapper (aExceptionMapper);
      aInvoker.registerAPI (aGetBusinessCard);
    }
    {
      final APIDescriptor aPutBusinessCard = new APIDescriptor (APIPath.put ("/businesscard/{" +
                                                                             PARAM_SERVICE_GROUP_ID +
                                                                             "}"),
                                                                new APIExecutorBusinessCardPut ());
      aPutBusinessCard.allowedMimeTypes ()
                      .addAll (CMimeType.TEXT_XML.getAsString (), CMimeType.APPLICATION_XML.getAsString ());
      aPutBusinessCard.setExceptionMapper (aExceptionMapper);
      aInvoker.registerAPI (aPutBusinessCard);
    }
    {
      final APIDescriptor aDeleteBusinessCard = new APIDescriptor (APIPath.delete ("/businesscard/{" +
                                                                                   PARAM_SERVICE_GROUP_ID +
                                                                                   "}"),
                                                                   new APIExecutorBusinessCardDelete ());
      aDeleteBusinessCard.setExceptionMapper (aExceptionMapper);
      aInvoker.registerAPI (aDeleteBusinessCard);
    }
    // CompleteServiceGroup
    {
      final APIDescriptor aGetCompleteServiceGroup = new APIDescriptor (APIPath.get ("/complete/{" +
                                                                                     PARAM_SERVICE_GROUP_ID +
                                                                                     "}"),
                                                                        new APIExecutorCompleteServiceGroupGet ());
      aGetCompleteServiceGroup.setExceptionMapper (aExceptionMapper);
      aInvoker.registerAPI (aGetCompleteServiceGroup);
    }
    // List
    {
      final APIDescriptor aGetList = new APIDescriptor (APIPath.get ("/list/{" + PARAM_USER_ID + "}"),
                                                        new APIExecutorListGet ());
      aGetList.setExceptionMapper (aExceptionMapper);
      aInvoker.registerAPI (aGetList);
    }
    // ServiceGroup
    {
      final APIDescriptor aGetServiceGroup = new APIDescriptor (APIPath.get ("/{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                                new APIExecutorServiceGroupGet ());
      aGetServiceGroup.setExceptionMapper (aExceptionMapper);
      aInvoker.registerAPI (aGetServiceGroup);
    }
    {
      final APIDescriptor aPutServiceGroup = new APIDescriptor (APIPath.put ("/{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                                new APIExecutorServiceGroupPut ());
      aPutServiceGroup.allowedMimeTypes ()
                      .addAll (CMimeType.TEXT_XML.getAsString (), CMimeType.APPLICATION_XML.getAsString ());
      aPutServiceGroup.setExceptionMapper (aExceptionMapper);
      aInvoker.registerAPI (aPutServiceGroup);
    }
    {
      final APIDescriptor aDeleteServiceGroup = new APIDescriptor (APIPath.delete ("/{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                                   new APIExecutorServiceGroupDelete ());
      aDeleteServiceGroup.setExceptionMapper (aExceptionMapper);
      aInvoker.registerAPI (aDeleteServiceGroup);
    }
    // ServiceMetadata
    {
      final APIDescriptor aGetServiceMetadata = new APIDescriptor (APIPath.get ("/{" +
                                                                                PARAM_SERVICE_GROUP_ID +
                                                                                "}/services/{" +
                                                                                PARAM_DOCUMENT_TYPE_ID +
                                                                                "}"),
                                                                   new APIExecutorServiceMetadataGet ());
      aGetServiceMetadata.setExceptionMapper (aExceptionMapper);
      aInvoker.registerAPI (aGetServiceMetadata);
    }
    {
      final APIDescriptor aPutServiceMetadata = new APIDescriptor (APIPath.put ("/{" +
                                                                                PARAM_SERVICE_GROUP_ID +
                                                                                "}/services/{" +
                                                                                PARAM_DOCUMENT_TYPE_ID +
                                                                                "}"),
                                                                   new APIExecutorServiceMetadataPut ());
      aPutServiceMetadata.allowedMimeTypes ()
                         .addAll (CMimeType.TEXT_XML.getAsString (), CMimeType.APPLICATION_XML.getAsString ());
      aPutServiceMetadata.setExceptionMapper (aExceptionMapper);
      aInvoker.registerAPI (aPutServiceMetadata);
    }
    {
      final APIDescriptor aDeleteServiceMetadata = new APIDescriptor (APIPath.delete ("/{" +
                                                                                      PARAM_SERVICE_GROUP_ID +
                                                                                      "}/services/{" +
                                                                                      PARAM_DOCUMENT_TYPE_ID +
                                                                                      "}"),
                                                                      new APIExecutorServiceMetadataDelete ());
      aDeleteServiceMetadata.setExceptionMapper (aExceptionMapper);
      aInvoker.registerAPI (aDeleteServiceMetadata);
    }
  }

  @Override
  @Nonnull
  protected EContinue onFilterBefore (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                      @Nonnull final UnifiedResponse aUnifiedResponse) throws IOException,
                                                                                       ServletException
  {
    final APIPath aAPIPath = APIPath.createForFilter (aRequestScope);

    if (aAPIPath.getPath ()
                .matches ("^/(stream|public|secure|ajax|resbundle|smp-status|error|logout|favicon.ico)(/.*)?$"))
    {
      // Explicitly other servlet
      if (LOGGER.isInfoEnabled ())
        LOGGER.info ("Ignoring '" + aAPIPath.getPath () + "' because it is an application servlet.");
      return EContinue.CONTINUE;
    }

    final GlobalAPIInvoker aInvoker = GlobalAPIInvoker.getInstance ();
    final InvokableAPIDescriptor aInvokableDescriptor = aInvoker.getAPIByPath (aAPIPath);
    if (aInvokableDescriptor == null)
    {
      // No API match
      return EContinue.CONTINUE;
    }

    if (LOGGER.isInfoEnabled ())
      LOGGER.info ("Found API match for '" + aAPIPath.getPath () + "': " + aInvokableDescriptor);

    if (aRequestScope.getHttpMethod () == EHttpMethod.GET)
      aUnifiedResponse.disableCaching ();

    // Invoke API and stop
    try
    {
      // Exception handler is handled internally
      aInvoker.invoke (aInvokableDescriptor, aRequestScope, aUnifiedResponse);
    }
    catch (final Exception ex)
    {
      // Re-throw
      if (ex instanceof IOException)
        throw (IOException) ex;
      if (ex instanceof ServletException)
        throw (ServletException) ex;
      throw new ServletException (ex);
    }

    return EContinue.BREAK;
  }
}
