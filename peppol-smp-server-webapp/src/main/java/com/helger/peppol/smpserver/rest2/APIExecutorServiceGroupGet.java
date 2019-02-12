package com.helger.peppol.smpserver.rest2;

import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.mime.CMimeType;
import com.helger.peppol.bdxr.marshal.BDXRMarshallerServiceGroupType;
import com.helger.peppol.smp.marshal.SMPMarshallerServiceGroupType;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.restapi.BDXRServerAPI;
import com.helger.peppol.smpserver.restapi.ISMPServerAPIDataProvider;
import com.helger.peppol.smpserver.restapi.SMPServerAPI;
import com.helger.photon.core.PhotonUnifiedResponse;
import com.helger.photon.core.api.IAPIDescriptor;
import com.helger.photon.core.api.IAPIExecutor;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class APIExecutorServiceGroupGet implements IAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorServiceGroupGet.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sServiceGroupID = aPathVariables.get (Rest2Filter.PARAM_SERVICE_GROUP_ID);
    final ISMPServerAPIDataProvider aDataProvider = new Rest2DataProvider (aRequestScope);

    byte [] aBytes;
    switch (SMPServerConfiguration.getRESTType ())
    {
      case PEPPOL:
      {
        final com.helger.peppol.smp.ServiceGroupType ret = new SMPServerAPI (aDataProvider).getServiceGroup (sServiceGroupID);
        aBytes = new SMPMarshallerServiceGroupType ().getAsBytes (ret);
        break;
      }
      case BDXR:
      {
        final com.helger.peppol.bdxr.ServiceGroupType ret = new BDXRServerAPI (aDataProvider).getServiceGroup (sServiceGroupID);
        aBytes = new BDXRMarshallerServiceGroupType ().getAsBytes (ret);
        break;
      }
      default:
        throw new UnsupportedOperationException ("Unsupported REST type specified!");
    }

    if (aBytes == null)
    {
      // Internal error serializing the payload
      LOGGER.warn ("Failed to convert the returned ServiceGroup to XML");
      aUnifiedResponse.createInternalServerError ();
    }
    else
    {
      aUnifiedResponse.setContent (aBytes).setMimeType (CMimeType.TEXT_XML);
    }
  }
}
