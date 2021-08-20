/**
 * Copyright (C) 2014-2021 Philip Helger and contributors
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
package com.helger.phoss.smp.rest2;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.http.EHttpMethod;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.regex.RegExHelper;
import com.helger.commons.state.EContinue;
import com.helger.photon.api.APIDescriptor;
import com.helger.photon.api.APIPath;
import com.helger.photon.api.GlobalAPIInvoker;
import com.helger.photon.api.IAPIExceptionMapper;
import com.helger.photon.api.IAPIRegistry;
import com.helger.photon.api.InvokableAPIDescriptor;
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
  public static final String PATH_BUSINESSCARD = "/businesscard/";
  public static final String PATH_COMPLETE = "/complete/";
  public static final String PATH_LIST = "/list/";
  public static final String PATH_SERVICES = "/services/";

  public static final String PATH_PREFIX_OASIS_BDXR_SMP_2 = "bdxr-smp-2";
  public static final String PARAM_SERVICE_GROUP_ID = "ServiceGroupId";
  public static final String PARAM_USER_ID = "UserId";
  public static final String PARAM_DOCUMENT_TYPE_ID = "DocumentTypeId";
  static final String LOG_PREFIX = "[REST API] ";

  private static final Logger LOGGER = LoggerFactory.getLogger (Rest2Filter.class);

  public Rest2Filter ()
  {
    final IAPIExceptionMapper aExceptionMapper = new Rest2ExceptionMapper ();
    final IAPIRegistry aAPIRegistry = GlobalAPIInvoker.getInstance ().getRegistry ();

    // BusinessCard
    {
      final APIDescriptor aGetBusinessCard = new APIDescriptor (APIPath.get (PATH_BUSINESSCARD + "{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                                new APIExecutorBusinessCardGet ());
      aGetBusinessCard.setExceptionMapper (aExceptionMapper);
      aAPIRegistry.registerAPI (aGetBusinessCard);
    }
    {
      final APIDescriptor aPutBusinessCard = new APIDescriptor (APIPath.put (PATH_BUSINESSCARD + "{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                                new APIExecutorBusinessCardPut ());
      aPutBusinessCard.allowedMimeTypes ().addAll (CMimeType.TEXT_XML.getAsString (), CMimeType.APPLICATION_XML.getAsString ());
      aPutBusinessCard.setExceptionMapper (aExceptionMapper);
      aAPIRegistry.registerAPI (aPutBusinessCard);
    }
    {
      final APIDescriptor aDeleteBusinessCard = new APIDescriptor (APIPath.delete (PATH_BUSINESSCARD + "{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                                   new APIExecutorBusinessCardDelete ());
      aDeleteBusinessCard.setExceptionMapper (aExceptionMapper);
      aAPIRegistry.registerAPI (aDeleteBusinessCard);
    }
    // CompleteServiceGroup
    {
      final APIDescriptor aGetCompleteServiceGroup = new APIDescriptor (APIPath.get (PATH_COMPLETE + "{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                                        new APIExecutorCompleteServiceGroupGet ());
      aGetCompleteServiceGroup.setExceptionMapper (aExceptionMapper);
      aAPIRegistry.registerAPI (aGetCompleteServiceGroup);
    }
    // List
    {
      final APIDescriptor aGetList = new APIDescriptor (APIPath.get (PATH_LIST + "{" + PARAM_USER_ID + "}"), new APIExecutorListGet ());
      aGetList.setExceptionMapper (aExceptionMapper);
      aAPIRegistry.registerAPI (aGetList);
    }
    // ServiceGroup
    {
      final APIDescriptor aGetServiceGroup = new APIDescriptor (APIPath.get ("/{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                                new APIExecutorServiceGroupGet ());
      aGetServiceGroup.setExceptionMapper (aExceptionMapper);
      aAPIRegistry.registerAPI (aGetServiceGroup);
    }
    {
      final APIDescriptor aPutServiceGroup = new APIDescriptor (APIPath.put ("/{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                                new APIExecutorServiceGroupPut ());
      aPutServiceGroup.allowedMimeTypes ().addAll (CMimeType.TEXT_XML.getAsString (), CMimeType.APPLICATION_XML.getAsString ());
      aPutServiceGroup.setExceptionMapper (aExceptionMapper);
      aAPIRegistry.registerAPI (aPutServiceGroup);
    }
    {
      final APIDescriptor aDeleteServiceGroup = new APIDescriptor (APIPath.delete ("/{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                                   new APIExecutorServiceGroupDelete ());
      aDeleteServiceGroup.setExceptionMapper (aExceptionMapper);
      aAPIRegistry.registerAPI (aDeleteServiceGroup);
    }
    // ServiceMetadata
    {
      final APIDescriptor aGetServiceMetadata = new APIDescriptor (APIPath.get ("/{" +
                                                                                PARAM_SERVICE_GROUP_ID +
                                                                                "}" +
                                                                                PATH_SERVICES +
                                                                                "{" +
                                                                                PARAM_DOCUMENT_TYPE_ID +
                                                                                "}"),
                                                                   new APIExecutorServiceMetadataGet ());
      aGetServiceMetadata.setExceptionMapper (aExceptionMapper);
      aAPIRegistry.registerAPI (aGetServiceMetadata);
    }
    {
      final APIDescriptor aPutServiceMetadata = new APIDescriptor (APIPath.put ("/{" +
                                                                                PARAM_SERVICE_GROUP_ID +
                                                                                "}" +
                                                                                PATH_SERVICES +
                                                                                "{" +
                                                                                PARAM_DOCUMENT_TYPE_ID +
                                                                                "}"),
                                                                   new APIExecutorServiceMetadataPut ());
      aPutServiceMetadata.allowedMimeTypes ().addAll (CMimeType.TEXT_XML.getAsString (), CMimeType.APPLICATION_XML.getAsString ());
      aPutServiceMetadata.setExceptionMapper (aExceptionMapper);
      aAPIRegistry.registerAPI (aPutServiceMetadata);
    }
    {
      final APIDescriptor aDeleteServiceMetadata = new APIDescriptor (APIPath.delete ("/{" +
                                                                                      PARAM_SERVICE_GROUP_ID +
                                                                                      "}" +
                                                                                      PATH_SERVICES +
                                                                                      "{" +
                                                                                      PARAM_DOCUMENT_TYPE_ID +
                                                                                      "}"),
                                                                      new APIExecutorServiceMetadataDelete ());
      aDeleteServiceMetadata.setExceptionMapper (aExceptionMapper);
      aAPIRegistry.registerAPI (aDeleteServiceMetadata);
    }
    {
      final APIDescriptor aDeleteAllServiceMetadata = new APIDescriptor (APIPath.delete ("/{" +
                                                                                         PARAM_SERVICE_GROUP_ID +
                                                                                         "}" +
                                                                                         PATH_SERVICES),
                                                                         new APIExecutorServiceMetadataDeleteAll ());
      aDeleteAllServiceMetadata.setExceptionMapper (aExceptionMapper);
      aAPIRegistry.registerAPI (aDeleteAllServiceMetadata);
    }

    // Extended Query APIs since 5.3.0
    {
      final APIDescriptor aSMPQueryEndpoints = new APIDescriptor (APIPath.get ("/smpquery/{" +
                                                                               PARAM_SERVICE_GROUP_ID +
                                                                               "}/{" +
                                                                               PARAM_DOCUMENT_TYPE_ID +
                                                                               "}"),
                                                                  new APIExecutorQueryGetServiceMetadata ());
      aSMPQueryEndpoints.setExceptionMapper (aExceptionMapper);
      aAPIRegistry.registerAPI (aSMPQueryEndpoints);
    }

    {
      final APIDescriptor aSMPQueryDocTypes = new APIDescriptor (APIPath.get ("/smpquery/{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                                 new APIExecutorQueryGetDocTypes ());
      aSMPQueryDocTypes.setExceptionMapper (aExceptionMapper);
      aAPIRegistry.registerAPI (aSMPQueryDocTypes);
    }

    {
      final APIDescriptor aSMPQueryBusinessCard = new APIDescriptor (APIPath.get ("/businesscardquery/{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                                     new APIExecutorQueryGetBusinessCard ());
      aSMPQueryBusinessCard.setExceptionMapper (aExceptionMapper);
      aAPIRegistry.registerAPI (aSMPQueryBusinessCard);
    }
  }

  @Override
  @Nonnull
  protected EContinue onFilterBefore (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                      @Nonnull final UnifiedResponse aUnifiedResponse) throws IOException, ServletException
  {
    if (LOGGER.isDebugEnabled ())
    {
      LOGGER.debug (aRequestScope.getRequest ().getRequestURI ());
      LOGGER.debug (aRequestScope.getRequest ().getRequestURL ().toString ());
      LOGGER.debug (aRequestScope.getRequestURIEncoded ());
      LOGGER.debug (aRequestScope.getRequestURLEncoded ().toString ());
      LOGGER.debug (aRequestScope.getRequestURIDecoded ());
      LOGGER.debug (aRequestScope.getRequestURLDecoded ().toString ());
    }
    final APIPath aAPIPath = APIPath.createForFilter (aRequestScope);

    // Hard coded path with white listed requests
    if (RegExHelper.stringMatchesPattern ("^/(ajax|error|favicon.ico|logout|public|resbundle|robots.txt|secure|smp-cspreporting|smp-status|stream)(/.*)?$",
                                          aAPIPath.getPath ()))
    {
      // Explicitly other servlet
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug (LOG_PREFIX + "Ignoring '" + aAPIPath.getPath () + "' because it is an application internal path.");
      return EContinue.CONTINUE;
    }

    final GlobalAPIInvoker aAPI = GlobalAPIInvoker.getInstance ();
    final InvokableAPIDescriptor aInvokableDescriptor = aAPI.getRegistry ().getAPIByPath (aAPIPath);
    if (aInvokableDescriptor == null)
    {
      // No API match
      return EContinue.CONTINUE;
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug (LOG_PREFIX + "Found API match for '" + aAPIPath.getPath () + "': " + aInvokableDescriptor);

    // Always disable caching for GET requests
    if (aRequestScope.getHttpMethod () == EHttpMethod.GET)
      aUnifiedResponse.disableCaching ();
    else
    {
      // If only a status code is provided, the caching option would be ignored
      // anyway, so for other HTTP methods the caching must be done explicitly
      // per request
    }

    // Invoke API and stop
    try
    {
      // Exception handler is handled internally
      aAPI.getInvoker ().invoke (aInvokableDescriptor, aRequestScope, aUnifiedResponse);
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
