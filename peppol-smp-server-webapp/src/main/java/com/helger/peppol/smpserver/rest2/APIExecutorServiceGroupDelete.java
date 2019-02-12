package com.helger.peppol.smpserver.rest2;

import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.restapi.BDXRServerAPI;
import com.helger.peppol.smpserver.restapi.ISMPServerAPIDataProvider;
import com.helger.peppol.smpserver.restapi.SMPServerAPI;
import com.helger.photon.core.PhotonUnifiedResponse;
import com.helger.photon.core.api.IAPIDescriptor;
import com.helger.photon.core.api.IAPIExecutor;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class APIExecutorServiceGroupDelete implements IAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorServiceGroupDelete.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      LOGGER.warn ("The writable REST API is disabled. deleteServiceGroup will not be executed.");
      aUnifiedResponse.createNotFound ();
    }
    else
    {
      final String sServiceGroupID = aPathVariables.get (Rest2Filter.PARAM_SERVICE_GROUP_ID);
      final ISMPServerAPIDataProvider aDataProvider = new Rest2DataProvider (aRequestScope);
      final BasicAuthClientCredentials aBasicAuth = Rest2RequestHelper.getAuth (aRequestScope.headers ());

      switch (SMPServerConfiguration.getRESTType ())
      {
        case PEPPOL:
          new SMPServerAPI (aDataProvider).deleteServiceGroup (sServiceGroupID, aBasicAuth);
          break;
        case BDXR:
          new BDXRServerAPI (aDataProvider).deleteServiceGroup (sServiceGroupID, aBasicAuth);
          break;
        default:
          throw new UnsupportedOperationException ("Unsupported REST type specified!");
      }
      aUnifiedResponse.createOk ();
    }
  }
}
