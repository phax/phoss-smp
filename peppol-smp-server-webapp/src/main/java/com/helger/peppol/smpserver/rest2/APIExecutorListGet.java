package com.helger.peppol.smpserver.rest2;

import java.util.Map;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.mime.CMimeType;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppol.bdxr.marshal.BDXRMarshallerServiceGroupReferenceListType;
import com.helger.peppol.smp.marshal.SMPMarshallerServiceGroupReferenceListType;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.restapi.BDXRServerAPI;
import com.helger.peppol.smpserver.restapi.ISMPServerAPIDataProvider;
import com.helger.peppol.smpserver.restapi.SMPServerAPI;
import com.helger.photon.core.api.IAPIDescriptor;
import com.helger.photon.core.api.IAPIExecutor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class APIExecutorListGet implements IAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorListGet.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sUserID = aPathVariables.get (Rest2Filter.PARAM_USER_ID);
    final ISMPServerAPIDataProvider aDataProvider = new Rest2DataProvider (aRequestScope);
    final BasicAuthClientCredentials aBasicAuth = Rest2RequestHelper.getAuth (aRequestScope.headers ());

    byte [] aBytes;
    switch (SMPServerConfiguration.getRESTType ())
    {
      case PEPPOL:
      {
        // Unspecified extension
        final com.helger.peppol.smp.ServiceGroupReferenceListType ret = new SMPServerAPI (aDataProvider).getServiceGroupReferenceList (sUserID,
                                                                                                                                       aBasicAuth);
        aBytes = new SMPMarshallerServiceGroupReferenceListType ().getAsBytes (ret);
        break;
      }
      case BDXR:
      {
        // Unspecified extension
        final com.helger.peppol.bdxr.ServiceGroupReferenceListType ret = new BDXRServerAPI (aDataProvider).getServiceGroupReferenceList (sUserID,
                                                                                                                                         aBasicAuth);
        aBytes = new BDXRMarshallerServiceGroupReferenceListType ().getAsBytes (ret);
        break;
      }
      default:
        throw new UnsupportedOperationException ("Unsupported REST type specified!");
    }

    if (aBytes == null)
    {
      // Internal error serializing the payload
      LOGGER.warn ("Failed to convert the returned CompleteServiceGroup to XML");
      aUnifiedResponse.setStatus (HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    else
    {
      aUnifiedResponse.setContent (aBytes).setMimeType (CMimeType.TEXT_XML);
    }
  }
}
