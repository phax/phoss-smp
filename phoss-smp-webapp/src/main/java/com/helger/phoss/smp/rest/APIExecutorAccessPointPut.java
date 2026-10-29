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
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.restapi.AccessPointServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.phoss.smp.restapi.SMPAccessPointRESTHelper;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;
import com.helger.xml.microdom.serialize.MicroReader;

/**
 * REST API executor for <code>PUT /accesspoint/name/{AccessPointName}</code>. Authenticated,
 * administrators only. Creates a new Access Point or updates an existing one.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public final class APIExecutorAccessPointPut extends AbstractSMPAPIExecutor
{
  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sAccessPointName = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_ACCESS_POINT_NAME));
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. accesspoint PUT will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    // Authenticate
    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());

    final byte [] aPayloadBytes = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
    if (aPayloadBytes == null)
      throw new SMPBadRequestException ("Failed to read request body", aDataProvider.getCurrentURI ());

    final IMicroDocument aRequestDoc = MicroReader.readMicroXML (aPayloadBytes);
    final IMicroElement eRequest = aRequestDoc == null ? null : aRequestDoc.getDocumentElement ();
    if (eRequest == null)
      throw new SMPBadRequestException ("Failed to parse the request body as XML", aDataProvider.getCurrentURI ());

    final String sEndpointReference = SMPAccessPointRESTHelper.getChildText (eRequest,
                                                                             SMPAccessPointRESTHelper.ELEMENT_ENDPOINT_REFERENCE);
    final String sCertificate = SMPAccessPointRESTHelper.getChildText (eRequest,
                                                                       SMPAccessPointRESTHelper.ELEMENT_CERTIFICATE);

    final ISMPAccessPoint aAccessPoint = new AccessPointServerAPI (aDataProvider).createOrUpdateAccessPoint (sAccessPointName,
                                                                                                             sEndpointReference,
                                                                                                             sCertificate,
                                                                                                             aCredentials);

    final IMicroDocument aDoc = new MicroDocument ();
    aDoc.addChild (SMPAccessPointRESTHelper.getAsMicroElement (aAccessPoint));

    aUnifiedResponse.xml (aDoc).disableCaching ();
  }
}
