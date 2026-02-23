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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.io.stream.StreamHelper;
import com.helger.base.string.StringHelper;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.sgprops.SGCustomPropertyList;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.security.user.IUser;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.convert.MicroTypeConverter;
import com.helger.xml.microdom.serialize.MicroReader;

/**
 * REST API executor for <code>PUT /{ServiceGroupId}/customproperties</code>. Authenticated.
 * Replaces all custom properties with the provided JSON array.
 *
 * @author Philip Helger
 * @since 8.1.0
 */
public final class APIExecutorCustomPropertiesPut extends AbstractSMPAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorCustomPropertiesPut.class);

  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sServiceGroupID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    // Is the writable API disabled?
    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. customproperties PUT will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    // Authenticate first
    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());
    final IUser aSMPUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);

    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aParticipantID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
    if (aParticipantID == null)
      throw new SMPBadRequestException ("Failed to parse participant identifier '" + sServiceGroupID + "'",
                                        aDataProvider.getCurrentURI ());

    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aParticipantID);
    if (aServiceGroup == null)
      throw new SMPNotFoundException ("No such service group '" + sServiceGroupID + "'",
                                      aDataProvider.getCurrentURI ());

    SMPUserManagerPhoton.verifyOwnership (aParticipantID, aSMPUser);

    // Read XML body
    final byte [] aPayload = StreamHelper.getAllBytes (aRequestScope.getRequest ().getInputStream ());
    if (aPayload == null || aPayload.length == 0)
      throw new SMPBadRequestException ("No request body provided", aDataProvider.getCurrentURI ());

    final IMicroDocument aDoc = MicroReader.readMicroXML (aPayload);
    if (aDoc == null || aDoc.getDocumentElement () == null)
      throw new SMPBadRequestException ("Failed to parse request body as XML document", aDataProvider.getCurrentURI ());

    final SGCustomPropertyList aCustomProperties = MicroTypeConverter.convertToNative (aDoc.getDocumentElement (),
                                                                                       SGCustomPropertyList.class);
    if (aCustomProperties == null)
      throw new SMPBadRequestException ("Failed to parse custom properties from XML", aDataProvider.getCurrentURI ());

    // Update the service group with the new custom properties
    aServiceGroupMgr.updateSMPServiceGroup (aParticipantID,
                                            aServiceGroup.getOwnerID (),
                                            aServiceGroup.getExtensions ().getExtensionsAsJsonString (),
                                            aCustomProperties);

    LOGGER.info (SMPRestFilter.LOG_PREFIX +
                 "PUT customproperties for '" +
                 sServiceGroupID +
                 "' - " +
                 aCustomProperties.size () +
                 " properties set");

    aUnifiedResponse.createOk ();
  }
}
