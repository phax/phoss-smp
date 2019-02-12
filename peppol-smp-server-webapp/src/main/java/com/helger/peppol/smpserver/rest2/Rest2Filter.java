package com.helger.peppol.smpserver.rest2;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.state.EContinue;
import com.helger.commons.state.EHandled;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.exception.SMPBadRequestException;
import com.helger.peppol.smpserver.exception.SMPInternalErrorException;
import com.helger.peppol.smpserver.exception.SMPNotFoundException;
import com.helger.peppol.smpserver.exception.SMPSMLException;
import com.helger.peppol.smpserver.exception.SMPServerException;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;
import com.helger.peppol.smpserver.exception.SMPUnknownUserException;
import com.helger.photon.core.api.APIDescriptor;
import com.helger.photon.core.api.APIDescriptorList;
import com.helger.photon.core.api.APIPath;
import com.helger.photon.core.api.AbstractAPIExceptionMapper;
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

  private final APIDescriptorList m_aAPIs = new APIDescriptorList ();

  public Rest2Filter ()
  {
    final IAPIExceptionMapper aExceptionMapper = new AbstractAPIExceptionMapper ()
    {
      @Nonnull
      public EHandled applyExceptionOnResponse (final InvokableAPIDescriptor aInvokableDescriptor,
                                                final IRequestWebScopeWithoutResponse aRequestScope,
                                                final UnifiedResponse aUnifiedResponse,
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
    {
      final APIDescriptor aGetBusinessCard = new APIDescriptor (APIPath.get ("/businesscard/{" +
                                                                             PARAM_SERVICE_GROUP_ID +
                                                                             "}"),
                                                                new APIExecutorBusinessCardGet ());
      aGetBusinessCard.setExceptionMapper (aExceptionMapper);
      m_aAPIs.addDescriptor (aGetBusinessCard);
    }
    {
      final APIDescriptor aPutBusinessCard = new APIDescriptor (APIPath.put ("/businesscard/{" +
                                                                             PARAM_SERVICE_GROUP_ID +
                                                                             "}"),
                                                                new APIExecutorBusinessCardPut ());
      aPutBusinessCard.allowedMimeTypes ()
                      .addAll (CMimeType.TEXT_XML.getAsString (), CMimeType.APPLICATION_XML.getAsString ());
      aPutBusinessCard.setExceptionMapper (aExceptionMapper);
      m_aAPIs.addDescriptor (aPutBusinessCard);
    }
    {
      final APIDescriptor aDeleteBusinessCard = new APIDescriptor (APIPath.delete ("/businesscard/{" +
                                                                                   PARAM_SERVICE_GROUP_ID +
                                                                                   "}"),
                                                                   new APIExecutorBusinessCardDelete ());
      aDeleteBusinessCard.setExceptionMapper (aExceptionMapper);
      m_aAPIs.addDescriptor (aDeleteBusinessCard);
    }
    // CompleteServiceGroup
    {
      final APIDescriptor aGetCompleteServiceGroup = new APIDescriptor (APIPath.get ("/complete/{" +
                                                                                     PARAM_SERVICE_GROUP_ID +
                                                                                     "}"),
                                                                        new APIExecutorCompleteServiceGroupGet ());
      aGetCompleteServiceGroup.setExceptionMapper (aExceptionMapper);
      m_aAPIs.addDescriptor (aGetCompleteServiceGroup);
    }
    // List
    {
      final APIDescriptor aGetList = new APIDescriptor (APIPath.get ("/list/{" + PARAM_USER_ID + "}"),
                                                        new APIExecutorListGet ());
      aGetList.setExceptionMapper (aExceptionMapper);
      m_aAPIs.addDescriptor (aGetList);
    }
    // ServiceGroup
    {
      final APIDescriptor aGetServiceGroup = new APIDescriptor (APIPath.get ("/{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                                new APIExecutorListGet ());
      aGetServiceGroup.setExceptionMapper (aExceptionMapper);
      m_aAPIs.addDescriptor (aGetServiceGroup);
    }
    {
      final APIDescriptor aPutServiceGroup = new APIDescriptor (APIPath.put ("/{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                                new APIExecutorServiceGroupPut ());
      aPutServiceGroup.allowedMimeTypes ()
                      .addAll (CMimeType.TEXT_XML.getAsString (), CMimeType.APPLICATION_XML.getAsString ());
      aPutServiceGroup.setExceptionMapper (aExceptionMapper);
      m_aAPIs.addDescriptor (aPutServiceGroup);
    }
    {
      final APIDescriptor aDeleteServiceGroup = new APIDescriptor (APIPath.delete ("/{" + PARAM_SERVICE_GROUP_ID + "}"),
                                                                   new APIExecutorServiceGroupDelete ());
      aDeleteServiceGroup.setExceptionMapper (aExceptionMapper);
      m_aAPIs.addDescriptor (aDeleteServiceGroup);
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
      m_aAPIs.addDescriptor (aGetServiceMetadata);
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
      m_aAPIs.addDescriptor (aPutServiceMetadata);
    }
    {
      final APIDescriptor aDeleteServiceMetadata = new APIDescriptor (APIPath.delete ("/{" +
                                                                                      PARAM_SERVICE_GROUP_ID +
                                                                                      "}/services/{" +
                                                                                      PARAM_DOCUMENT_TYPE_ID +
                                                                                      "}"),
                                                                      new APIExecutorServiceMetadataDelete ());
      aDeleteServiceMetadata.setExceptionMapper (aExceptionMapper);
      m_aAPIs.addDescriptor (aDeleteServiceMetadata);
    }
  }

  @Override
  @Nonnull
  protected EContinue onFilterBefore (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                                      @Nonnull final UnifiedResponse aUnifiedResponse) throws IOException,
                                                                                       ServletException
  {
    final APIPath aAPIPath = APIPath.createFromRequest (aRequestScope);
    final InvokableAPIDescriptor aInvokableDescriptor = m_aAPIs.getMatching (aAPIPath);
    if (aInvokableDescriptor == null)
    {
      // No API match
      return EContinue.CONTINUE;
    }

    // Invoke API and stop
    try
    {
      aInvokableDescriptor.invokeAPI (aRequestScope, aUnifiedResponse);
    }
    catch (final Throwable t)
    {
      boolean bHandled = false;
      final IAPIExceptionMapper aExMapper = aInvokableDescriptor.getAPIDescriptor ().getExceptionMapper ();
      if (aExMapper != null)
      {
        // Apply exception mapper
        bHandled = aExMapper.applyExceptionOnResponse (aInvokableDescriptor, aRequestScope, aUnifiedResponse, t)
                            .isHandled ();
      }

      if (!bHandled)
      {
        // Re-throw
        if (t instanceof IOException)
          throw (IOException) t;
        if (t instanceof ServletException)
          throw (ServletException) t;
        throw new ServletException (t);
      }
    }

    return EContinue.BREAK;
  }
}
