package com.helger.peppol.smpserver.rest2;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.state.EHandled;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.exception.SMPBadRequestException;
import com.helger.peppol.smpserver.exception.SMPInternalErrorException;
import com.helger.peppol.smpserver.exception.SMPNotFoundException;
import com.helger.peppol.smpserver.exception.SMPSMLException;
import com.helger.peppol.smpserver.exception.SMPServerException;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;
import com.helger.peppol.smpserver.exception.SMPUnknownUserException;
import com.helger.photon.core.PhotonUnifiedResponse;
import com.helger.photon.core.api.APIDescriptor;
import com.helger.photon.core.api.APIPath;
import com.helger.photon.core.api.AbstractAPIExceptionMapper;
import com.helger.photon.core.api.IAPIExceptionMapper;
import com.helger.photon.core.api.InvokableAPIDescriptor;
import com.helger.servlet.filter.AbstractHttpServletFilter;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public class Rest2Filter extends AbstractHttpServletFilter
{
  public static final String PARAM_SERVICE_GROUP_ID = "ServiceGroupId";
  public static final String PARAM_USER_ID = "UserId";
  public static final String PARAM_DOCUMENT_TYPE_ID = "DocumentTypeId";

  private static final Logger LOGGER = LoggerFactory.getLogger (Rest2Filter.class);

  public Rest2Filter ()
  {
    final IAPIExceptionMapper aExceptionMapper = new AbstractAPIExceptionMapper ()
    {
      @Nonnull
      public EHandled applyExceptionOnResponse (final InvokableAPIDescriptor aInvokableDescriptor,
                                                final IRequestWebScopeWithoutResponse aRequestScope,
                                                final PhotonUnifiedResponse aUnifiedResponse,
                                                final Throwable aThrowable)
      {
        // From specific to general
        if (aThrowable instanceof SMPUnauthorizedException)
        {
          if (SMPServerConfiguration.isRESTLogExceptions ())
            LOGGER.error ("Unauthorized", aThrowable);
          setSimpleTextResponse (aUnifiedResponse,
                                 HttpServletResponse.SC_FORBIDDEN,
                                 getResponseEntityWithoutStackTrace (aThrowable));
          return EHandled.HANDLED;
        }
        if (aThrowable instanceof SMPUnknownUserException)
        {
          if (SMPServerConfiguration.isRESTLogExceptions ())
            LOGGER.error ("Unknown user", aThrowable);
          setSimpleTextResponse (aUnifiedResponse,
                                 HttpServletResponse.SC_FORBIDDEN,
                                 getResponseEntityWithoutStackTrace (aThrowable));
          return EHandled.HANDLED;
        }
        if (aThrowable instanceof SMPSMLException)
        {
          if (SMPServerConfiguration.isRESTLogExceptions ())
            LOGGER.error ("SMP SML error", aThrowable);
          setSimpleTextResponse (aUnifiedResponse,
                                 HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                 GlobalDebug.isDebugMode () ? getResponseEntityWithStackTrace (aThrowable)
                                                            : getResponseEntityWithoutStackTrace (aThrowable));
          return EHandled.HANDLED;
        }
        if (aThrowable instanceof SMPNotFoundException)
        {
          if (SMPServerConfiguration.isRESTLogExceptions ())
            LOGGER.error ("Not found", aThrowable);
          setSimpleTextResponse (aUnifiedResponse,
                                 HttpServletResponse.SC_NOT_FOUND,
                                 getResponseEntityWithoutStackTrace (aThrowable));
          return EHandled.HANDLED;
        }
        if (aThrowable instanceof SMPInternalErrorException)
        {
          if (SMPServerConfiguration.isRESTLogExceptions ())
            LOGGER.error ("Internal error", aThrowable);
          setSimpleTextResponse (aUnifiedResponse,
                                 HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                 GlobalDebug.isDebugMode () ? getResponseEntityWithStackTrace (aThrowable)
                                                            : getResponseEntityWithoutStackTrace (aThrowable));
          return EHandled.HANDLED;
        }
        if (aThrowable instanceof SMPBadRequestException)
        {
          if (SMPServerConfiguration.isRESTLogExceptions ())
            LOGGER.error ("Bad request", aThrowable);
          setSimpleTextResponse (aUnifiedResponse,
                                 HttpServletResponse.SC_BAD_REQUEST,
                                 getResponseEntityWithoutStackTrace (aThrowable));
          return EHandled.HANDLED;
        }
        if (aThrowable instanceof SMPServerException)
        {
          // Generic fallback only
          if (SMPServerConfiguration.isRESTLogExceptions ())
            LOGGER.error ("Generic SMP server", aThrowable);
          setSimpleTextResponse (aUnifiedResponse,
                                 HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                 getResponseEntityWithoutStackTrace (aThrowable));
          return EHandled.HANDLED;
        }
        if (aThrowable instanceof RuntimeException)
        {
          if (SMPServerConfiguration.isRESTLogExceptions ())
            LOGGER.error ("Runtime exception - " + aThrowable.getClass ().getName (), aThrowable);
          setSimpleTextResponse (aUnifiedResponse,
                                 HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                 GlobalDebug.isDebugMode () ? getResponseEntityWithStackTrace (aThrowable)
                                                            : getResponseEntityWithoutStackTrace (aThrowable));
          return EHandled.HANDLED;
        }

        return EHandled.UNHANDLED;
      }
    };

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
