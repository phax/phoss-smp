/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phoss.smp.rest;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.CGlobal;
import com.helger.base.codec.base64.Base64;
import com.helger.base.string.StringHelper;
import com.helger.base.timing.StopWatch;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.rt.OffsetDate;
import com.helger.datetime.xml.XMLOffsetDate;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.peppol.sml.ESMPAPIType;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.ui.types.smp.SMPQueryParams;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.smpclient.bdxr1.BDXRClientReadOnly;
import com.helger.smpclient.bdxr2.BDXR2ClientReadOnly;
import com.helger.smpclient.extension.SMPExtensionList;
import com.helger.smpclient.json.SMPJsonResponse;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xsds.bdxr.smp2.ac.CertificateType;

import jakarta.annotation.Nullable;

public final class APIExecutorQueryGetServiceMetadata extends AbstractSMPAPIExecutorQuery
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorQueryGetServiceMetadata.class);

  @Nullable
  public static String getLD (@Nullable final OffsetDate aLD)
  {
    return aLD == null ? null : DateTimeFormatter.ISO_LOCAL_DATE.format (aLD);
  }

  @Nullable
  public static String getLD (@Nullable final XMLOffsetDate aLD)
  {
    return aLD == null ? null : DateTimeFormatter.ISO_LOCAL_DATE.format (aLD);
  }

  @NonNull
  static IJsonObject convertEndpoint (final com.helger.xsds.bdxr.smp2.ac.@NonNull EndpointType aEndpoint)
  {
    final IJsonObject ret = new JsonObject ();

    ret.addIfNotNull (SMPJsonResponse.JSON_TRANSPORT_PROFILE, aEndpoint.getTransportProfileIDValue ())
       .addIfNotNull (SMPJsonResponse.JSON_SERVICE_DESCRIPTION, aEndpoint.getDescription ())
       .addIfNotNull (SMPJsonResponse.JSON_TECHNICAL_CONTACT_URL, aEndpoint.getContactValue ())
       .addIfNotNull (SMPJsonResponse.JSON_ENDPOINT_REFERENCE, aEndpoint.getAddressURIValue ())
       .addIfNotNull (SMPJsonResponse.JSON_SERVICE_ACTIVATION_DATE, getLD (aEndpoint.getActivationDateValue ()))
       .addIfNotNull (SMPJsonResponse.JSON_SERVICE_EXPIRATION_DATE, getLD (aEndpoint.getExpirationDateValue ()));

    final IJsonArray aJsonCerts = new JsonArray ();
    for (final CertificateType aCert : aEndpoint.getCertificate ())
    {
      final IJsonObject aJsonCert = new JsonObject ();
      SMPJsonResponse.convertCertificate (aJsonCert, Base64.encodeBytes (aCert.getContentBinaryObjectValue ()));
      aJsonCerts.add (aJsonCert);
    }
    ret.add ("certificates", aJsonCerts);

    final SMPExtensionList aExts = SMPExtensionList.ofBDXR2 (aEndpoint.getSMPExtensions ());
    if (aExts != null)
    {
      // It's okay to add as string
      ret.addIfNotNull (SMPJsonResponse.JSON_EXTENSION, aExts.getExtensionsAsJsonString ());
    }
    return ret;
  }

  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sLogPrefix = "[QueryAPI] ";
    final String sPathServiceGroupID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    // Is the remote query API disabled?
    if (SMPServerConfiguration.isRestRemoteQueryAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The remote query API is disabled. getRemoteServiceInformation will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    final IIdentifierFactory aIF = SMPMetaManager.getIdentifierFactory ();
    final ESMPAPIType eAPIType = SMPServerConfiguration.getRESTType ().getAPIType ();
    final ISMLInfo aSMLInfo = SMPMetaManager.getSettings ().getSMLInfo ();

    final IParticipantIdentifier aParticipantID = aIF.parseParticipantIdentifier (sPathServiceGroupID);
    if (aParticipantID == null)
    {
      throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, aDataProvider.getCurrentURI ());
    }

    final SMPQueryParams aSMPQueryParams = SMPQueryParams.createForSMLOrNull (aSMLInfo,
                                                                              eAPIType,
                                                                              aIF,
                                                                              aParticipantID.getScheme (),
                                                                              aParticipantID.getValue (),
                                                                              true);
    if (aSMPQueryParams == null)
    {
      LOGGER.error (sLogPrefix + "Failed to perform the SMP lookup");
      aUnifiedResponse.createNotFound ();
      return;
    }

    final String sDocTypeID = aPathVariables.get (SMPRestFilter.PARAM_DOCUMENT_TYPE_ID);
    final IDocumentTypeIdentifier aDocTypeID = aIF.parseDocumentTypeIdentifier (sDocTypeID);
    if (aDocTypeID == null)
      throw SMPBadRequestException.failedToParseDocType (sDocTypeID, null);

    final boolean bXMLSchemaValidation = aRequestScope.params ().getAsBoolean ("xmlSchemaValidation", true);
    final boolean bVerifySignature = aRequestScope.params ().getAsBoolean ("verifySignature", true);

    final ZonedDateTime aQueryDT = PDTFactory.getCurrentZonedDateTimeUTC ();
    final StopWatch aSW = StopWatch.createdStarted ();

    LOGGER.info (sLogPrefix +
                 "Participant information of '" +
                 aParticipantID.getURIEncoded () +
                 "' is queried using SMP API '" +
                 eAPIType +
                 "' from '" +
                 aSMPQueryParams.getSMPHostURI () +
                 "' for document type '" +
                 aDocTypeID.getURIEncoded () +
                 "'; XSD validation=" +
                 bXMLSchemaValidation +
                 "; signature verification=" +
                 bVerifySignature);

    IJsonObject aJson = null;
    switch (eAPIType)
    {
      case PEPPOL:
      {
        final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (aSMPQueryParams.getSMPHostURI ());
        aSMPClient.setXMLSchemaValidation (bXMLSchemaValidation);
        aSMPClient.setVerifySignature (bVerifySignature);

        final var aSSM = aSMPClient.getServiceMetadataOrNull (aParticipantID, aDocTypeID);
        if (aSSM != null)
        {
          final com.helger.xsds.peppol.smp1.ServiceMetadataType aSM = aSSM.getServiceMetadata ();
          aJson = SMPJsonResponse.convert (aParticipantID, aDocTypeID, aSM);
        }
        break;
      }
      case OASIS_BDXR_V1:
      {
        final BDXRClientReadOnly aBDXR1Client = new BDXRClientReadOnly (aSMPQueryParams.getSMPHostURI ());
        aBDXR1Client.setXMLSchemaValidation (bXMLSchemaValidation);
        aBDXR1Client.setVerifySignature (bVerifySignature);

        final var aSSM = aBDXR1Client.getServiceMetadataOrNull (aParticipantID, aDocTypeID);
        if (aSSM != null)
        {
          final com.helger.xsds.bdxr.smp1.ServiceMetadataType aSM = aSSM.getServiceMetadata ();
          aJson = SMPJsonResponse.convert (aParticipantID, aDocTypeID, aSM);
        }
        break;
      }
      case OASIS_BDXR_V2:
      {
        final BDXR2ClientReadOnly aBDXR2Client = new BDXR2ClientReadOnly (aSMPQueryParams.getSMPHostURI ());
        aBDXR2Client.setXMLSchemaValidation (bXMLSchemaValidation);
        aBDXR2Client.setVerifySignature (bVerifySignature);

        final var aSM = aBDXR2Client.getServiceMetadataOrNull (aParticipantID, aDocTypeID);
        if (aSM != null)
        {
          aJson = SMPJsonResponse.convert (aParticipantID, aDocTypeID, aSM);
        }
        break;
      }
    }

    aSW.stop ();

    if (aJson == null)
    {
      LOGGER.error (sLogPrefix + "Failed to perform the SMP lookup");
      aUnifiedResponse.createNotFound ();
    }
    else
    {
      LOGGER.info (sLogPrefix + "Succesfully finished lookup lookup after " + aSW.getMillis () + " milliseconds");

      aJson.add ("queryDateTime", DateTimeFormatter.ISO_ZONED_DATE_TIME.format (aQueryDT));
      aJson.add ("queryDurationMillis", aSW.getMillis ());

      aUnifiedResponse.json (aJson).enableCaching (1 * CGlobal.SECONDS_PER_HOUR);
    }
  }
}
