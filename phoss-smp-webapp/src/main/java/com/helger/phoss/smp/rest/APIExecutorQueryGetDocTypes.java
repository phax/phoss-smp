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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.CGlobal;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsTreeMap;
import com.helger.commons.collection.impl.ICommonsSortedMap;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.http.CHttp;
import com.helger.commons.mime.CMimeType;
import com.helger.commons.string.StringHelper;
import com.helger.commons.timing.StopWatch;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.response.ResponseHandlerByteArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonWriter;
import com.helger.json.serialize.JsonWriterSettings;
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
import com.helger.servlet.response.UnifiedResponse;
import com.helger.smpclient.bdxr1.BDXRClientReadOnly;
import com.helger.smpclient.bdxr2.BDXR2ClientReadOnly;
import com.helger.smpclient.json.SMPJsonResponse;
import com.helger.smpclient.peppol.SMPClientReadOnly;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public final class APIExecutorQueryGetDocTypes extends AbstractSMPAPIExecutorQuery
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorQueryGetDocTypes.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sPathServiceGroupID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope, sPathServiceGroupID);

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
    {
      throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, aDataProvider.getCurrentURI ());
    }
    final SMPQueryParams aQueryParams = SMPQueryParams.create (eAPIType, aParticipantID);

    final boolean bQueryBusinessCard = aRequestScope.params ().getAsBoolean ("businessCard", false);
    final boolean bXMLSchemaValidation = aRequestScope.params ().getAsBoolean ("xmlSchemaValidation", true);

    final ZonedDateTime aQueryDT = PDTFactory.getCurrentZonedDateTimeUTC ();
    final StopWatch aSW = StopWatch.createdStarted ();

    final String sLogPrefix = "[QueryAPI] ";

    LOGGER.info (sLogPrefix +
                 "Document types of '" +
                 aParticipantID.getURIEncoded () +
                 "' are queried using SMP API '" +
                 eAPIType +
                 "' from '" +
                 aQueryParams.getSMPHostURI () +
                 "'; XSD validation=" +
                 bXMLSchemaValidation);

    ICommonsSortedMap <String, String> aSGHrefs = null;
    switch (eAPIType)
    {
      case PEPPOL:
      {
        final SMPClientReadOnly aSMPClient = new SMPClientReadOnly (aQueryParams.getSMPHostURI ());
        aSMPClient.setXMLSchemaValidation (bXMLSchemaValidation);

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
        final BDXRClientReadOnly aBDXR1Client = new BDXRClientReadOnly (aQueryParams.getSMPHostURI ());
        aBDXR1Client.setXMLSchemaValidation (bXMLSchemaValidation);

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
        final BDXR2ClientReadOnly aBDXR2Client = new BDXR2ClientReadOnly (aQueryParams.getSMPHostURI ());
        aBDXR2Client.setXMLSchemaValidation (bXMLSchemaValidation);

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
      aJson = SMPJsonResponse.convert (eAPIType, aParticipantID, aSGHrefs, aIF);
    if (bQueryBusinessCard)
    {
      final String sBCURL = aQueryParams.getSMPHostURI ().toString () +
                            "/businesscard/" +
                            aParticipantID.getURIEncoded ();
      LOGGER.info (sLogPrefix + "Querying BC from '" + sBCURL + "'");

      byte [] aData;
      try (HttpClientManager aHttpClientMgr = new HttpClientManager ())
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
        final PDBusinessCard aBC = PDBusinessCardHelper.parseBusinessCard (aData, (Charset) null);
        if (aBC == null)
        {
          LOGGER.error (sLogPrefix + "Failed to parse BC:\n" + new String (aData, StandardCharsets.UTF_8));
        }
        else
        {
          // Business Card found
          if (aJson == null)
            aJson = new JsonObject ();
          aJson.addJson ("businessCard", aBC.getAsJson ());
        }
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
