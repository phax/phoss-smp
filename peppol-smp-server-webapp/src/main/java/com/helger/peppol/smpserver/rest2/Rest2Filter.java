package com.helger.peppol.smpserver.rest2;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.mime.CMimeType;
import com.helger.photon.core.api.APIDescriptor;
import com.helger.photon.core.api.APIPath;
import com.helger.servlet.filter.AbstractHttpServletFilter;

public class Rest2Filter extends AbstractHttpServletFilter
{
  public static final String PARAM_SERVICE_GROUP_ID = "ServiceGroupId";
  public static final String PARAM_USER_ID = "UserId";
  public static final String PARAM_DOCUMENT_TYPE_ID = "DocumentTypeId";

  private static final Logger LOGGER = LoggerFactory.getLogger (Rest2Filter.class);

  public Rest2Filter ()
  {
    // BusinessCard
    final APIDescriptor aGetBusinessCard = new APIDescriptor (APIPath.get ("/businesscard/{" +
                                                                           PARAM_SERVICE_GROUP_ID +
                                                                           "}"),
                                                              new APIExecutorBusinessCardGet ());

    final APIDescriptor aPutBusinessCard = new APIDescriptor (APIPath.put ("/businesscard/{" +
                                                                           PARAM_SERVICE_GROUP_ID +
                                                                           "}"),
                                                              new APIExecutorBusinessCardPut ());
    aPutBusinessCard.allowedMimeTypes ()
                    .addAll (CMimeType.TEXT_XML.getAsString (), CMimeType.APPLICATION_XML.getAsString ());

    final APIDescriptor aDeleteBusinessCard = new APIDescriptor (APIPath.delete ("/businesscard/{" +
                                                                                 PARAM_SERVICE_GROUP_ID +
                                                                                 "}"),
                                                                 new APIExecutorBusinessCardDelete ());

    // CompleteServiceGroup
    final APIDescriptor aGetCompleteServiceGroup = new APIDescriptor (APIPath.get ("/complete/{" +
                                                                                   PARAM_SERVICE_GROUP_ID +
                                                                                   "}"),
                                                                      new APIExecutorCompleteServiceGroupGet ());

    // List
    final APIDescriptor aGetList = new APIDescriptor (APIPath.get ("/list/{" + PARAM_USER_ID + "}"),
                                                      new APIExecutorListGet ());

    // ServiceGroup
    final APIDescriptor aGetServiceGroup = new APIDescriptor (APIPath.get ("/{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                              new APIExecutorListGet ());

    final APIDescriptor aPutServiceGroup = new APIDescriptor (APIPath.put ("/{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                              new APIExecutorServiceGroupPut ());
    aPutServiceGroup.allowedMimeTypes ()
                    .addAll (CMimeType.TEXT_XML.getAsString (), CMimeType.APPLICATION_XML.getAsString ());

    final APIDescriptor aDeleteServiceGroup = new APIDescriptor (APIPath.delete ("/{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                                 new APIExecutorServiceGroupDelete ());

    // ServiceMetadata
    final APIDescriptor aGetServiceMetadata = new APIDescriptor (APIPath.get ("/{" +
                                                                              PARAM_SERVICE_GROUP_ID +
                                                                              "}/services/{" +
                                                                              PARAM_DOCUMENT_TYPE_ID +
                                                                              "}"),
                                                                 new APIExecutorServiceMetadataGet ());

    final APIDescriptor aPutServiceMetadata = new APIDescriptor (APIPath.put ("/{" +
                                                                              PARAM_SERVICE_GROUP_ID +
                                                                              "}/services/{" +
                                                                              PARAM_DOCUMENT_TYPE_ID +
                                                                              "}"),
                                                                 new APIExecutorServiceMetadataPut ());
    aPutServiceMetadata.allowedMimeTypes ()
                       .addAll (CMimeType.TEXT_XML.getAsString (), CMimeType.APPLICATION_XML.getAsString ());

    final APIDescriptor aDeleteServiceMetadata = new APIDescriptor (APIPath.delete ("/{" +
                                                                                    PARAM_SERVICE_GROUP_ID +
                                                                                    "}/services/{" +
                                                                                    PARAM_DOCUMENT_TYPE_ID +
                                                                                    "}"),
                                                                    new APIExecutorServiceMetadataDelete ());
  }

  @Override
  public void doHttpFilter (final HttpServletRequest aHttpRequest,
                            final HttpServletResponse aHttpResponse,
                            final FilterChain aChain) throws IOException, ServletException
  {}
}
