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
import com.helger.base.array.ArrayHelper;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHelper;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.sgprops.SGCustomPropertyList;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.restapi.CustomPropertiesServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.convert.MicroTypeConverter;
import com.helger.xml.microdom.serialize.MicroReader;

/**
 * REST API executor for <code>PUT /{ServiceGroupId}/customproperties</code>. Authenticated.
 * Replaces all custom properties with the provided XML body.
 *
 * @author Philip Helger
 * @since 8.1.0
 */
public final class APIExecutorCustomPropertiesPut extends AbstractSMPAPIExecutor
{
  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sPathServiceGroupID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. customproperties PUT will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    // Authenticate
    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());

    // Read XML body
    final byte [] aPayloadBytes = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
    if (ArrayHelper.isEmpty (aPayloadBytes))
      throw new SMPBadRequestException ("No request body provided", aDataProvider.getCurrentURI ());

    final IMicroDocument aDoc = MicroReader.readMicroXML (aPayloadBytes);
    if (aDoc == null || aDoc.getDocumentElement () == null)
      throw new SMPBadRequestException ("Failed to parse request body as XML document", aDataProvider.getCurrentURI ());

    // Custom property name and value are checked when reading the XML
    final SGCustomPropertyList aCustomProperties = MicroTypeConverter.convertToNative (aDoc.getDocumentElement (),
                                                                                       SGCustomPropertyList.class);
    if (aCustomProperties == null)
      throw new SMPBadRequestException ("Failed to parse custom properties from XML", aDataProvider.getCurrentURI ());

    new CustomPropertiesServerAPI (aDataProvider).setCustomProperties (sPathServiceGroupID,
                                                                       aCustomProperties,
                                                                       aCredentials);

    aUnifiedResponse.createNoContent ();
  }
}
