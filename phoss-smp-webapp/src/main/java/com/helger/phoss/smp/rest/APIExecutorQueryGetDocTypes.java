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

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.CGlobal;
import com.helger.base.string.StringHelper;
import com.helger.base.timing.StopWatch;
import com.helger.collection.commons.CommonsTreeMap;
import com.helger.collection.commons.ICommonsSortedMap;
import com.helger.datetime.helper.PDTFactory;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.peppol.businesscard.generic.PDBusinessCard;
import com.helger.peppol.businesscard.helper.PDBusinessCardHelper;
import com.helger.peppol.sml.ESMPAPIType;
import com.helger.peppolid.CIdentifier;
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
import com.helger.smpclient.httpclient.SMPHttpClientSettings;
import com.helger.smpclient.json.SMPJsonResponse;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.annotation.Nonnull;

public final class APIExecutorQueryGetDocTypes extends AbstractSMPAPIExecutorQuery
{
  public static final String PARAM_XML_SCHEMA_VALIDATION = "xmlSchemaValidation";
  public static final String PARAM_BUSINESS_CARD = "businessCard";

  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorQueryGetDocTypes.class);

  @Override
  protected void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                            @Nonnull @Nonempty final String sPath,
                            @Nonnull final Map <String, String> aPathVariables,
                            @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                            @Nonnull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sLogPrefix = "[QueryAPI] ";

    final String sPathServiceGroupID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    // Is the remote query API disabled?
    if (SMPServerConfiguration.isRestRemoteQueryAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The remote query API is disabled. getRemoteDocTypes will not be executed",
                                                aDataProvider.getCurrentURI ());
    }
    final IIdentifierFactory aIF = SMPMetaManager.getIdentifierFactory ();
    final ESMPAPIType eAPIType = SMPServerConfiguration.getRESTType ().getAPIType ();

    final IParticipantIdentifier aParticipantID = aIF.parseParticipantIdentifier (sPathServiceGroupID);
    if (aParticipantID == null)
      throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, aDataProvider.getCurrentURI ());

    final SMPQueryParams aSMPQueryParams = SMPQueryParams.create (eAPIType, aParticipantID);
    if (aSMPQueryParams == null)
    {
      LOGGER.error (sLogPrefix + "Participant ID '" + sPathServiceGroupID + "' is not registered in the DNS");
      aUnifiedResponse.createNotFound ();
      return;
    }

    final boolean bQueryBusinessCard = aRequestScope.params ().getAsBoolean (PARAM_BUSINESS_CARD, false);
    final boolean bXMLSchemaValidation = aRequestScope.params ().getAsBoolean (PARAM_XML_SCHEMA_VALIDATION, true);
    final boolean bVerifySignature = true;

    final ZonedDateTime aQueryDT = PDTFactory.getCurrentZonedDateTimeUTC ();
    final StopWatch aSW = StopWatch.createdStarted ();

    LOGGER.info (sLogPrefix +
                 "Document types of '" +
                 aParticipantID.getURIEncoded () +
                 "' are queried using SMP API '" +
                 eAPIType +
                 "' from '" +
                 aSMPQueryParams.getSMPHostURI () +
                 "'; XSD validation=" +
                 bXMLSchemaValidation +
                 "; signature verification=" +
                 bVerifySignature);

    ICommonsSortedMap <String, String> aSGHrefs = null;
    switch (eAPIType)
    {
      case PEPPOL:
      {
        final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (aSMPQueryParams.getSMPHostURI ());
        aSMPClient.setXMLSchemaValidation (bXMLSchemaValidation);
        aSMPClient.setVerifySignature (bVerifySignature);

        // Get all HRefs and sort them by decoded URL
        final com.helger.xsds.peppol.smp1.ServiceGroupType aSG = aSMPClient.getServiceGroupOrNull (aParticipantID);
        // Map from cleaned URL to original URL
        if (aSG != null && aSG.getServiceMetadataReferenceCollection () != null)
        {
          aSGHrefs = new CommonsTreeMap <> ();
          for (final com.helger.xsds.peppol.smp1.ServiceMetadataReferenceType aSMR : aSG.getServiceMetadataReferenceCollection ()
                                                                                        .getServiceMetadataReference ())
          {
            // Decoded href is important for unification
            final String sHref = CIdentifier.createPercentDecoded (aSMR.getHref ());
            if (aSGHrefs.put (sHref, aSMR.getHref ()) != null)
              LOGGER.warn (sLogPrefix + "The ServiceGroup list contains the duplicate URL '" + sHref + "'");
          }
        }
        break;
      }
      case OASIS_BDXR_V1:
      {
        aSGHrefs = new CommonsTreeMap <> ();
        final BDXRClientReadOnly aBDXR1Client = new BDXRClientReadOnly (aSMPQueryParams.getSMPHostURI ());
        aBDXR1Client.setXMLSchemaValidation (bXMLSchemaValidation);
        aBDXR1Client.setVerifySignature (bVerifySignature);

        // Get all HRefs and sort them by decoded URL
        final com.helger.xsds.bdxr.smp1.ServiceGroupType aSG = aBDXR1Client.getServiceGroupOrNull (aParticipantID);
        // Map from cleaned URL to original URL
        if (aSG != null && aSG.getServiceMetadataReferenceCollection () != null)
        {
          aSGHrefs = new CommonsTreeMap <> ();
          for (final com.helger.xsds.bdxr.smp1.ServiceMetadataReferenceType aSMR : aSG.getServiceMetadataReferenceCollection ()
                                                                                      .getServiceMetadataReference ())
          {
            // Decoded href is important for unification
            final String sHref = CIdentifier.createPercentDecoded (aSMR.getHref ());
            if (aSGHrefs.put (sHref, aSMR.getHref ()) != null)
              LOGGER.warn (sLogPrefix + "The ServiceGroup list contains the duplicate URL '" + sHref + "'");
          }
        }
        break;
      }
      case OASIS_BDXR_V2:
      {
        aSGHrefs = new CommonsTreeMap <> ();
        final BDXR2ClientReadOnly aBDXR2Client = new BDXR2ClientReadOnly (aSMPQueryParams.getSMPHostURI ());
        aBDXR2Client.setXMLSchemaValidation (bXMLSchemaValidation);
        aBDXR2Client.setVerifySignature (bVerifySignature);

        // Get all HRefs and sort them by decoded URL
        final com.helger.xsds.bdxr.smp2.ServiceGroupType aSG = aBDXR2Client.getServiceGroupOrNull (aParticipantID);
        // Map from cleaned URL to original URL
        if (aSG != null && aSG.hasServiceReferenceEntries ())
        {
          aSGHrefs = new CommonsTreeMap <> ();
          for (final com.helger.xsds.bdxr.smp2.ac.ServiceReferenceType aSR : aSG.getServiceReference ())
          {
            // Decoded href is important for unification
            final String sSrcID = CIdentifier.getURIEncodedBDXR2 (aSR.getID ().getSchemeID (),
                                                                  aSR.getID ().getValue ());
            final String sHref = CIdentifier.createPercentDecoded (sSrcID);
            if (aSGHrefs.put (sHref, sSrcID) != null)
              LOGGER.warn (sLogPrefix + "The ServiceGroup list contains the duplicate URL '" + sHref + "'");
          }
        }
        break;
      }
    }
    IJsonObject aJson = null;
    if (aSGHrefs != null)
    {
      aJson = SMPJsonResponse.convert (eAPIType, aParticipantID, aSGHrefs, aIF);
    }

    if (bQueryBusinessCard)
    {
      final SMPHttpClientSettings aHCS = new SMPHttpClientSettings ();

      final String sBCURL = StringHelper.trimEnd (aSMPQueryParams.getSMPHostURI ().toString (), '/') +
                            "/businesscard/" +
                            aParticipantID.getURIEncoded ();
      LOGGER.info (sLogPrefix + "Querying BC from '" + sBCURL + "'");

      byte [] aData;
      try (final HttpClientManager aHttpClientMgr = HttpClientManager.create (aHCS))
      {
        final HttpGet aGet = new HttpGet (sBCURL);
        aData = aHttpClientMgr.execute (aGet, new ResponseHandlerByteArray ());
      }
      catch (final Exception ex)
      {
        aData = null;
      }

      if (aData == null)
      {
        LOGGER.warn (sLogPrefix + "No Business Card is available for that participant.");
      }
      else
      {
        final PDBusinessCard aBC = PDBusinessCardHelper.parseBusinessCard (aData, StandardCharsets.UTF_8);
        if (aBC == null)
        {
          LOGGER.error (sLogPrefix + "Failed to parse BC:\n" + new String (aData, StandardCharsets.UTF_8));
        }
        else
        {
          // Business Card found
          if (aJson == null)
            aJson = new JsonObject ();
          aJson.add (PARAM_BUSINESS_CARD, aBC.getAsJson ());
        }
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
