package com.helger.peppol.smpserver.rest2;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.state.ESuccess;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppol.bdxr.marshal.BDXRMarshallerServiceGroupType;
import com.helger.peppol.smp.marshal.SMPMarshallerServiceGroupType;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.restapi.BDXRServerAPI;
import com.helger.peppol.smpserver.restapi.ISMPServerAPIDataProvider;
import com.helger.peppol.smpserver.restapi.SMPServerAPI;
import com.helger.photon.core.api.IAPIDescriptor;
import com.helger.photon.core.api.IAPIExecutor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.read.DOMReader;

public final class APIExecutorServiceGroupPut implements IAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorServiceGroupPut.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      LOGGER.warn ("The writable REST API is disabled. saveServiceGroup will not be executed.");
      aUnifiedResponse.setStatus (HttpServletResponse.SC_NOT_FOUND);
    }
    else
    {
      // Parse main payload
      final byte [] aPayload = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
      final Document aServiceGroupDoc = DOMReader.readXMLDOM (aPayload);
      if (aServiceGroupDoc == null)
      {
        LOGGER.warn ("Failed to parse provided payload as XML.");
        aUnifiedResponse.setStatus (HttpServletResponse.SC_BAD_REQUEST);
      }
      else
      {
        final String sServiceGroupID = aPathVariables.get (Rest2Filter.PARAM_SERVICE_GROUP_ID);
        final ISMPServerAPIDataProvider aDataProvider = new Rest2DataProvider (aRequestScope);
        final BasicAuthClientCredentials aBasicAuth = Rest2RequestHelper.getAuth (aRequestScope.headers ());

        ESuccess eSuccess = ESuccess.FAILURE;
        switch (SMPServerConfiguration.getRESTType ())
        {
          case PEPPOL:
          {
            // TODO use logging exception handler
            final com.helger.peppol.smp.ServiceGroupType aServiceGroup = new SMPMarshallerServiceGroupType ().read (aServiceGroupDoc);
            if (aServiceGroup != null)
            {
              new SMPServerAPI (aDataProvider).saveServiceGroup (sServiceGroupID, aServiceGroup, aBasicAuth);
              eSuccess = ESuccess.SUCCESS;
            }
            break;
          }
          case BDXR:
          {
            // TODO use logging exception handler
            final com.helger.peppol.bdxr.ServiceGroupType aServiceGroup = new BDXRMarshallerServiceGroupType ().read (aServiceGroupDoc);
            if (aServiceGroup != null)
            {
              new BDXRServerAPI (aDataProvider).saveServiceGroup (sServiceGroupID, aServiceGroup, aBasicAuth);
              eSuccess = ESuccess.SUCCESS;
            }
            break;
          }
          default:
            throw new UnsupportedOperationException ("Unsupported REST type specified!");
        }
        if (eSuccess.isFailure ())
          aUnifiedResponse.setStatus (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        else
          aUnifiedResponse.setStatus (HttpServletResponse.SC_OK);
      }
    }
  }
}
