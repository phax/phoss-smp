package com.helger.peppol.smpserver.rest2;

import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.state.ESuccess;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppol.bdxr.marshal.BDXRMarshallerServiceMetadataType;
import com.helger.peppol.smp.marshal.SMPMarshallerServiceMetadataType;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.restapi.BDXRServerAPI;
import com.helger.peppol.smpserver.restapi.ISMPServerAPIDataProvider;
import com.helger.peppol.smpserver.restapi.SMPServerAPI;
import com.helger.photon.core.PhotonUnifiedResponse;
import com.helger.photon.core.api.IAPIDescriptor;
import com.helger.photon.core.api.IAPIExecutor;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.serialize.read.DOMReader;

public final class APIExecutorServiceMetadataPut implements IAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorServiceMetadataPut.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    /*
     * Is the writable API disabled?
     */
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      LOGGER.warn ("The writable REST API is disabled. saveServiceRegistration will not be executed.");
      aUnifiedResponse.createNotFound ();
    }
    else
    {
      // Parse main payload
      final byte [] aPayload = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
      final Document aServiceMetadataDoc = DOMReader.readXMLDOM (aPayload);
      if (aServiceMetadataDoc == null)
      {
        LOGGER.warn ("Failed to parse provided payload as XML.");
        aUnifiedResponse.createBadRequest ();
      }
      else
      {
        final String sServiceGroupID = aPathVariables.get (Rest2Filter.PARAM_SERVICE_GROUP_ID);
        final String sDocumentTypeID = aPathVariables.get (Rest2Filter.PARAM_DOCUMENT_TYPE_ID);
        final ISMPServerAPIDataProvider aDataProvider = new Rest2DataProvider (aRequestScope);
        final BasicAuthClientCredentials aBasicAuth = Rest2RequestHelper.getAuth (aRequestScope.headers ());

        ESuccess eSuccess = ESuccess.FAILURE;
        switch (SMPServerConfiguration.getRESTType ())
        {
          case PEPPOL:
          {
            final com.helger.peppol.smp.ServiceMetadataType aServiceMetadata = new SMPMarshallerServiceMetadataType ().read (aServiceMetadataDoc);
            if (aServiceMetadata != null)
              eSuccess = new SMPServerAPI (aDataProvider).saveServiceRegistration (sServiceGroupID,
                                                                                   sDocumentTypeID,
                                                                                   aServiceMetadata,
                                                                                   aBasicAuth);
            break;
          }
          case BDXR:
          {
            final com.helger.peppol.bdxr.ServiceMetadataType aServiceMetadata = new BDXRMarshallerServiceMetadataType ().read (aServiceMetadataDoc);
            if (aServiceMetadata != null)
              eSuccess = new BDXRServerAPI (aDataProvider).saveServiceRegistration (sServiceGroupID,
                                                                                    sDocumentTypeID,
                                                                                    aServiceMetadata,
                                                                                    aBasicAuth);
            break;
          }
          default:
            throw new UnsupportedOperationException ("Unsupported REST type specified!");
        }

        if (eSuccess.isFailure ())
          aUnifiedResponse.createBadRequest ();
        else
          aUnifiedResponse.createOk ();
      }
    }
  }
}
