/*
 * Copyright (C) 2014-2024 Philip Helger and contributors
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

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.CGlobal;
import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.base64.Base64;
import com.helger.commons.datetime.OffsetDate;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.XMLOffsetDate;
import com.helger.commons.http.CHttp;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.timing.StopWatch;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.JsonValue;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.peppol.sml.ESMPAPIType;
import com.helger.peppolid.CIdentifier;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.smpclient.bdxr1.BDXRClientReadOnly;
import com.helger.smpclient.bdxr2.BDXR2ClientReadOnly;
import com.helger.smpclient.extension.SMPExtensionList;
import com.helger.smpclient.json.SMPJsonResponse;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xsds.bdxr.smp2.ac.CertificateType;
import com.helger.xsds.bdxr.smp2.ac.ProcessMetadataType;
import com.helger.xsds.bdxr.smp2.ac.ProcessType;

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

  @Nonnull
  static IJsonObject convertEndpoint (@Nonnull final com.helger.xsds.bdxr.smp2.ac.EndpointType aEndpoint)
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
    ret.addJson ("certificates", aJsonCerts);

    final SMPExtensionList aExts = SMPExtensionList.ofBDXR2 (aEndpoint.getSMPExtensions ());
    if (aExts != null)
    {
      // It's okay to add as string
      ret.addIfNotNull (SMPJsonResponse.JSON_EXTENSION, aExts.getExtensionsAsJsonString ());
    }
    return ret;
  }

  // TODO use SMPJsonResponse version in peppol-commons>8.8.0
  @Nonnull
  static IJsonObject convert (@Nonnull final IParticipantIdentifier aParticipantID,
                              @Nonnull final IDocumentTypeIdentifier aDocTypeID,
                              @Nonnull final com.helger.xsds.bdxr.smp2.ServiceMetadataType aSM)
  {
    ValueEnforcer.notNull (aParticipantID, "ParticipantID");
    ValueEnforcer.notNull (aDocTypeID, "DocTypeID");
    ValueEnforcer.notNull (aSM, "SM");

    final IJsonObject ret = new JsonObject ();
    ret.add (SMPJsonResponse.JSON_SMPTYPE, ESMPAPIType.OASIS_BDXR_V2.getID ());
    ret.add (SMPJsonResponse.JSON_PARTICIPANT_ID, aParticipantID.getURIEncoded ());
    ret.add (SMPJsonResponse.JSON_DOCUMENT_TYPE_ID, aDocTypeID.getURIEncoded ());

    for (final ProcessMetadataType aPM : aSM.getProcessMetadata ())
    {
      final IJsonObject aJsonProcessMetadata = new JsonObject ();

      {
        final SMPExtensionList aExts = SMPExtensionList.ofBDXR2 (aPM.getSMPExtensions ());
        if (aExts != null)
          aJsonProcessMetadata.addIfNotNull (SMPJsonResponse.JSON_EXTENSION, aExts.getExtensionsAsJsonString ());
      }

      {
        // Convert all process IDs
        final IJsonArray aJsonProcesses = new JsonArray ();
        for (final ProcessType aProc : aPM.getProcess ())
        {
          final IJsonObject aJsonProc = new JsonObject ().add ("id",
                                                               CIdentifier.getURIEncodedBDXR2 (aProc.getID ()
                                                                                                    .getSchemeID (),
                                                                                               aProc.getID ()
                                                                                                    .getValue ()));
          if (aProc.hasRoleIDEntries ())
          {
            aJsonProc.add ("roleids",
                           new JsonArray ().addAllMapped (aProc.getRoleID (),
                                                          x -> JsonValue.create (CIdentifier.getURIEncodedBDXR2 (x.getSchemeID (),
                                                                                                                 x.getValue ()))));
          }

          final SMPExtensionList aExts = SMPExtensionList.ofBDXR2 (aProc.getSMPExtensions ());
          if (aExts != null)
            aJsonProc.addIfNotNull (SMPJsonResponse.JSON_EXTENSION, aExts.getExtensionsAsJsonString ());

          aJsonProcesses.add (aJsonProc);
        }
        aJsonProcessMetadata.add ("processes", aJsonProcesses);
      }

      final com.helger.xsds.bdxr.smp2.ac.RedirectType aRedirect = aPM.getRedirect ();
      if (aRedirect != null)
      {
        final IJsonObject aJsonRedirect = new JsonObject ().add (SMPJsonResponse.JSON_HREF,
                                                                 aRedirect.getPublisherURIValue ());

        // Add all certificates
        final IJsonArray aJsonCerts = new JsonArray ();
        for (final CertificateType aCert : aRedirect.getCertificate ())
        {
          final IJsonObject aJsonCert = new JsonObject ();
          SMPJsonResponse.convertCertificate (aJsonCert, Base64.encodeBytes (aCert.getContentBinaryObjectValue ()));
          aJsonCerts.add (aJsonCert);
        }
        aJsonRedirect.addJson ("certificates", aJsonCerts);

        final SMPExtensionList aExts = SMPExtensionList.ofBDXR2 (aRedirect.getSMPExtensions ());
        if (aExts != null)
        {
          // It's okay to add as string
          aJsonRedirect.addIfNotNull (SMPJsonResponse.JSON_EXTENSION, aExts.getExtensionsAsJsonString ());
        }
        aJsonProcessMetadata.addJson (SMPJsonResponse.JSON_REDIRECT, aJsonRedirect);
      }
      else
      {
        final IJsonArray aJsonEPs = new JsonArray ();
        // For all endpoints
        for (final com.helger.xsds.bdxr.smp2.ac.EndpointType aEndpoint : aPM.getEndpoint ())
        {
          aJsonEPs.add (convertEndpoint (aEndpoint));
        }
        aJsonProcessMetadata.addJson (SMPJsonResponse.JSON_ENDPOINTS, aJsonEPs);
      }
    }

    final SMPExtensionList aExts = SMPExtensionList.ofBDXR2 (aSM.getSMPExtensions ());
    if (aExts != null)
    {
      // It's okay to add as string
      ret.addIfNotNull (SMPJsonResponse.JSON_EXTENSION, aExts.getExtensionsAsJsonString ());
    }
    return ret;
  }

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sPathServiceGroupID = aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID);
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope, sPathServiceGroupID);

    // Is the remote query API disabled?
    if (SMPServerConfiguration.isRestRemoteQueryAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The remote query API is disabled. getRemoteServiceInformation will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    final IIdentifierFactory aIF = SMPMetaManager.getIdentifierFactory ();
    final ESMPAPIType eAPIType = SMPServerConfiguration.getRESTType ().getAPIType ();

    final IParticipantIdentifier aParticipantID = aIF.parseParticipantIdentifier (sPathServiceGroupID);
    if (aParticipantID == null)
    {
      throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, aDataProvider.getCurrentURI ());
    }

    final SMPQueryParams aQueryParams = SMPQueryParams.create (eAPIType, aParticipantID);

    final String sDocTypeID = aPathVariables.get (SMPRestFilter.PARAM_DOCUMENT_TYPE_ID);
    final IDocumentTypeIdentifier aDocTypeID = aIF.parseDocumentTypeIdentifier (sDocTypeID);
    if (aDocTypeID == null)
      throw SMPBadRequestException.failedToParseDocType (sDocTypeID, null);

    final boolean bXMLSchemaValidation = aRequestScope.params ().getAsBoolean ("xmlSchemaValidation", true);
    final boolean bVerifySignature = aRequestScope.params ().getAsBoolean ("verifySignature", true);

    final ZonedDateTime aQueryDT = PDTFactory.getCurrentZonedDateTimeUTC ();
    final StopWatch aSW = StopWatch.createdStarted ();

    final String sLogPrefix = "[QueryAPI] ";
    LOGGER.info (sLogPrefix +
                 "Participant information of '" +
                 aParticipantID.getURIEncoded () +
                 "' is queried using SMP API '" +
                 eAPIType +
                 "' from '" +
                 aQueryParams.getSMPHostURI () +
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
        final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (aQueryParams.getSMPHostURI ());
        aSMPClient.setXMLSchemaValidation (bXMLSchemaValidation);
        aSMPClient.setVerifySignature (bVerifySignature);

        final com.helger.xsds.peppol.smp1.SignedServiceMetadataType aSSM = aSMPClient.getServiceMetadataOrNull (aParticipantID,
                                                                                                                aDocTypeID);
        if (aSSM != null)
        {
          final com.helger.xsds.peppol.smp1.ServiceMetadataType aSM = aSSM.getServiceMetadata ();
          aJson = SMPJsonResponse.convert (aParticipantID, aDocTypeID, aSM);
        }
        break;
      }
      case OASIS_BDXR_V1:
      {
        final BDXRClientReadOnly aBDXR1Client = new BDXRClientReadOnly (aQueryParams.getSMPHostURI ());
        aBDXR1Client.setXMLSchemaValidation (bXMLSchemaValidation);
        aBDXR1Client.setVerifySignature (bVerifySignature);

        final com.helger.xsds.bdxr.smp1.SignedServiceMetadataType aSSM = aBDXR1Client.getServiceMetadataOrNull (aParticipantID,
                                                                                                                aDocTypeID);
        if (aSSM != null)
        {
          final com.helger.xsds.bdxr.smp1.ServiceMetadataType aSM = aSSM.getServiceMetadata ();
          aJson = SMPJsonResponse.convert (aParticipantID, aDocTypeID, aSM);
        }
        break;
      }
      case OASIS_BDXR_V2:
      {
        final BDXR2ClientReadOnly aBDXR2Client = new BDXR2ClientReadOnly (aQueryParams.getSMPHostURI ());
        aBDXR2Client.setXMLSchemaValidation (bXMLSchemaValidation);
        aBDXR2Client.setVerifySignature (bVerifySignature);

        final com.helger.xsds.bdxr.smp2.ServiceMetadataType aSM = aBDXR2Client.getServiceMetadataOrNull (aParticipantID,
                                                                                                         aDocTypeID);
        if (aSM != null)
        {
          aJson = convert (aParticipantID, aDocTypeID, aSM);
        }
        break;
      }
    }

    aSW.stop ();

    if (aJson == null)
    {
      LOGGER.error (sLogPrefix + "Failed to perform the SMP lookup");
      aUnifiedResponse.setStatus (CHttp.HTTP_NOT_FOUND);
    }
    else
    {
      LOGGER.info (sLogPrefix + "Succesfully finished lookup lookup after " + aSW.getMillis () + " milliseconds");

      aJson.add ("queryDateTime", DateTimeFormatter.ISO_ZONED_DATE_TIME.format (aQueryDT));
      aJson.add ("queryDurationMillis", aSW.getMillis ());

      final String sRet = new JsonWriter (JsonWriterSettings.DEFAULT_SETTINGS_FORMATTED).writeAsString (aJson);
      aUnifiedResponse.setContentAndCharset (sRet, StandardCharsets.UTF_8)
                      .setMimeType (CMimeType.APPLICATION_JSON)
                      .enableCaching (1 * CGlobal.SECONDS_PER_HOUR);
    }
  }
}
