/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
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

import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.http.CHttp;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.string.StringHelper;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroReader;
import com.helger.xml.microdom.util.MicroHelper;

/**
 * REST API to perform an inbound migration for a participant
 *
 * @author Philip Helger
 * @since 5.6.0
 */
public final class APIExecutorMigrationInboundFromXMLPut extends AbstractSMPAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorMigrationInboundFromXMLPut.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sLogPrefix = "[Migration-Inbound-XML] ";

    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      LOGGER.warn (sLogPrefix + "The writable REST API is disabled. migrationInbound will not be executed.");
      aUnifiedResponse.setStatus (CHttp.HTTP_PRECONDITION_FAILED);
    }
    else
    {
      final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope, null);

      // Parse main payload
      final byte [] aPayload = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
      final IMicroDocument aMigrateDoc = MicroReader.readMicroXML (aPayload);
      if (aMigrateDoc == null || aMigrateDoc.getDocumentElement () == null)
      {
        LOGGER.warn (sLogPrefix + "Failed to parse provided payload as XML.");
        aUnifiedResponse.setStatus (CHttp.HTTP_BAD_REQUEST);
      }
      else
      {
        final String sServiceGroupID = MicroHelper.getChildTextContent (aMigrateDoc.getDocumentElement (),
                                                                        APIExecutorMigrationOutboundStartPut.XML_ELEMENT_PARTICIPANT_ID);
        if (StringHelper.hasNoText (sServiceGroupID))
          throw new SMPBadRequestException ("The XML payload is missing the '" +
                                            APIExecutorMigrationOutboundStartPut.XML_ELEMENT_PARTICIPANT_ID +
                                            "' element content.",
                                            aDataProvider.getCurrentURI ());

        final String sMigrationKey = MicroHelper.getChildTextContent (aMigrateDoc.getDocumentElement (),
                                                                      APIExecutorMigrationOutboundStartPut.XML_ELEMENT_MIGRATION_KEY);
        if (StringHelper.hasNoText (sMigrationKey))
          throw new SMPBadRequestException ("The XML payload is missing the '" +
                                            APIExecutorMigrationOutboundStartPut.XML_ELEMENT_MIGRATION_KEY +
                                            "' element content.",
                                            aDataProvider.getCurrentURI ());

        APIExecutorMigrationInboundFromPathPut.migrationInbound (sServiceGroupID,
                                                                 sMigrationKey,
                                                                 sLogPrefix,
                                                                 aRequestScope,
                                                                 aUnifiedResponse);
      }
    }
  }
}
