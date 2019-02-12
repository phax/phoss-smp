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
  private static final Logger LOGGER = LoggerFactory.getLogger (Rest2Filter.class);

  public Rest2Filter ()
  {
    final APIDescriptor aGetBusinessCard = new APIDescriptor (APIPath.get ("/businesscard/{" +
                                                                           PARAM_SERVICE_GROUP_ID +
                                                                           "}"),
                                                              new APIExecutorBusinessCardGet ());

    final APIDescriptor aSaveBusinessCard = new APIDescriptor (APIPath.post ("/businesscard/{" +
                                                                             PARAM_SERVICE_GROUP_ID +
                                                                             "}"),
                                                               new APIExecutorBusinessCardPost ());
    aSaveBusinessCard.allowedMimeTypes ()
                     .addAll (CMimeType.TEXT_XML.getAsString (), CMimeType.APPLICATION_XML.getAsString ());

    final APIDescriptor aDeleteBusinessCard = new APIDescriptor (APIPath.delete ("/businesscard/{" +
                                                                                 PARAM_SERVICE_GROUP_ID +
                                                                                 "}"),
                                                                 new APIExecutorBusinessCardDelete ());
  }

  @Override
  public void doHttpFilter (final HttpServletRequest aHttpRequest,
                            final HttpServletResponse aHttpResponse,
                            final FilterChain aChain) throws IOException, ServletException
  {}
}
