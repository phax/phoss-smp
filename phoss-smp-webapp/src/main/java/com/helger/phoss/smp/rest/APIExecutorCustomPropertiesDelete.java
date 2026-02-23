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

/**
 * REST API executor for <code>DELETE /{ServiceGroupId}/customproperties</code>. Authenticated.
 * Deletes all custom properties.
 *
 * @author Philip Helger
 * @since 8.1.0
 */
public final class APIExecutorCustomPropertiesDelete extends AbstractSMPAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorCustomPropertiesDelete.class);

  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sPathServiceGroupID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    if (SMPMetaManager.getSettings ().isRESTWritableAPIDisabled ())
    {
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. customproperties DELETE will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    // Authenticate
    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());
    final IUser aSMPUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);

    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aParticipantID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
    if (aParticipantID == null)
      throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, aDataProvider.getCurrentURI ());

    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aParticipantID);
    if (aServiceGroup == null)
      throw SMPNotFoundException.unknownSG (sPathServiceGroupID, aDataProvider.getCurrentURI ());

    SMPUserManagerPhoton.verifyOwnership (aParticipantID, aSMPUser);

    // Remove the property
    final SGCustomPropertyList aCustomProperties = aServiceGroup.getCustomProperties ();
    int nDeletedProperties;
    if (aCustomProperties != null && aCustomProperties.isNotEmpty ())
    {
      nDeletedProperties = aCustomProperties.size ();
      // Update the service group but setting no properties
      aServiceGroupMgr.updateSMPServiceGroup (aParticipantID,
                                              aServiceGroup.getOwnerID (),
                                              aServiceGroup.getExtensions ().getExtensionsAsJsonString (),
                                              null);
    }
    else
      nDeletedProperties = 0;

    LOGGER.info (SMPRestFilter.LOG_PREFIX +
                 "DELETE customproperties (" +
                 nDeletedProperties +
                 ") '" +
                 sPathServiceGroupID +
                 "'");

    aUnifiedResponse.text (Integer.toString (nDeletedProperties));
  }
}
