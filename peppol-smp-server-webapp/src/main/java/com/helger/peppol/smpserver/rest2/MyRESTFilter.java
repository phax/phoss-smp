package com.helger.peppol.smpserver.rest2;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.mime.CMimeType;
import com.helger.pd.businesscard.v3.PD3BusinessCardMarshaller;
import com.helger.pd.businesscard.v3.PD3BusinessCardType;
import com.helger.peppol.smpserver.app.SMPWebAppConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.restapi.BusinessCardServerAPI;
import com.helger.peppol.smpserver.restapi.ISMPServerAPIDataProvider;
import com.helger.photon.core.api.APIDescriptor;
import com.helger.photon.core.api.APIPath;
import com.helger.photon.core.api.IAPIExecutor;
import com.helger.servlet.filter.AbstractHttpServletFilter;

public class MyRESTFilter extends AbstractHttpServletFilter
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MyRESTFilter.class);
  private static final String PARAM_SERVICE_GROUP_ID = "ServiceGroupId";

  static
  {}

  @Override
  public void doHttpFilter (final HttpServletRequest aHttpRequest,
                            final HttpServletResponse aHttpResponse,
                            final FilterChain aChain) throws IOException, ServletException
  {
    final String sPathBusinessCard = "/businesscard/{" + PARAM_SERVICE_GROUP_ID + "}";
    final IAPIExecutor aGetBusinessCardExec = (aAPIDescriptor,
                                               sPath,
                                               aPathVariables,
                                               aRequestScope,
                                               aUnifiedResponse) -> {
      if (!SMPMetaManager.getSettings ().isPEPPOLDirectoryIntegrationEnabled ())
      {
        /*
         * PD integration is disabled
         */
        LOGGER.warn ("The " +
                     SMPWebAppConfiguration.getDirectoryName () +
                     " integration is disabled. getBusinessCard will not be executed.");
        aUnifiedResponse.createNotFound ();
      }
      else
      {
        final String sServiceGroupID = aPathVariables.get (PARAM_SERVICE_GROUP_ID);
        final ISMPServerAPIDataProvider aDataProvider = new SMPMyRESTAPIDataProvider (aRequestScope);
        /*
         * getBusinessCard throws an exception if non is found
         */
        final PD3BusinessCardType ret = new BusinessCardServerAPI (aDataProvider).getBusinessCard (sServiceGroupID);
        final byte [] aBytes = new PD3BusinessCardMarshaller ().getAsBytes (ret);
        aUnifiedResponse.setContent (aBytes);
        aUnifiedResponse.setMimeType (CMimeType.TEXT_XML);
      }
    };
    final APIDescriptor aGetBusinessCard = new APIDescriptor (APIPath.get (sPathBusinessCard), aGetBusinessCardExec);
  }

}
