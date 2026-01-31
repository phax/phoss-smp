/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHelper;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
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
  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sLogPrefix = "[REST API Migration-Inbound-XML] ";
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. migrationInbound will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    // Parse main payload
    final byte [] aPayload = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
    final IMicroDocument aMigrateDoc = MicroReader.readMicroXML (aPayload);
    if (aMigrateDoc == null || aMigrateDoc.getDocumentElement () == null)
    {
      throw new SMPBadRequestException ("Failed to parse provided payload as XML", aDataProvider.getCurrentURI ());
    }

    final String sServiceGroupID = MicroHelper.getChildTextContent (aMigrateDoc.getDocumentElement (),
                                                                    APIExecutorMigrationOutboundStartPut.XML_ELEMENT_PARTICIPANT_ID);
    if (StringHelper.isEmpty (sServiceGroupID))
      throw new SMPBadRequestException ("The XML payload is missing the '" +
                                        APIExecutorMigrationOutboundStartPut.XML_ELEMENT_PARTICIPANT_ID +
                                        "' element content.",
                                        aDataProvider.getCurrentURI ());

    final String sMigrationKey = MicroHelper.getChildTextContent (aMigrateDoc.getDocumentElement (),
                                                                  APIExecutorMigrationOutboundStartPut.XML_ELEMENT_MIGRATION_KEY);
    if (StringHelper.isEmpty (sMigrationKey))
      throw new SMPBadRequestException ("The XML payload is missing the '" +
                                        APIExecutorMigrationOutboundStartPut.XML_ELEMENT_MIGRATION_KEY +
                                        "' element content.",
                                        aDataProvider.getCurrentURI ());

    // Response is filled inside
    APIExecutorMigrationInboundFromPathPut.migrationInbound (sServiceGroupID,
                                                             sMigrationKey,
                                                             sLogPrefix,
                                                             aRequestScope,
                                                             aUnifiedResponse);
  }
}
