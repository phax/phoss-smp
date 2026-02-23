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
import com.helger.phoss.smp.domain.sgprops.SGCustomProperty;
import com.helger.phoss.smp.domain.sgprops.SGCustomPropertyList;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPUnauthorizedException;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.security.user.IUser;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * REST API executor for <code>GET /{ServiceGroupId}/customproperties/{PropertyName}</code>. Returns
 * only the values of public properties for unauthenticated requests; returns the value of each
 * property if authenticated.
 *
 * @author Philip Helger
 * @since 8.1.0
 */
public final class APIExecutorCustomPropertyGet extends AbstractSMPAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorCustomPropertyGet.class);

  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sPathServiceGroupID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final String sPathPropertyName = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_CUSTOM_PROPERTY_NAME));
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final IParticipantIdentifier aParticipantID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
    if (aParticipantID == null)
      throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, aDataProvider.getCurrentURI ());

    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aParticipantID);
    if (aServiceGroup == null)
      throw SMPNotFoundException.unknownSG (sPathServiceGroupID, aDataProvider.getCurrentURI ());

    // Check if authenticated - if so, return all properties; otherwise only public
    boolean bAuthenticated = false;
    try
    {
      final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());
      final IUser aSMPUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);
      SMPUserManagerPhoton.verifyOwnership (aParticipantID, aSMPUser);
      bAuthenticated = true;
    }
    catch (final SMPUnauthorizedException ex)
    {
      // Not authenticated - that's fine for GET
      // Only the public properties will be listed
    }

    final SGCustomPropertyList aCustomProperties = aServiceGroup.getCustomProperties ();
    final SGCustomProperty aCustomProperty = aCustomProperties == null ? null : bAuthenticated ? aCustomProperties
                                                                                                                  .findFirst (x -> x.getName ()
                                                                                                                                    .equals (sPathPropertyName))
                                                                                               : aCustomProperties.findFirst (x -> x.isPublic () &&
                                                                                                                                   x.getName ()
                                                                                                                                    .equals (sPathPropertyName));
    if (aCustomProperty == null)
      throw new SMPNotFoundException ("Custom property '" +
                                      sPathPropertyName +
                                      "' not found in Service Group '" +
                                      sPathServiceGroupID +
                                      "'",
                                      aDataProvider.getCurrentURI ());

    LOGGER.info (SMPRestFilter.LOG_PREFIX +
                 "GET CustomProperty" +
                 (bAuthenticated ? " [authenticated]" : "") +
                 " for '" +
                 sPathServiceGroupID +
                 "' and property '" +
                 sPathPropertyName +
                 "'");

    aUnifiedResponse.text (aCustomProperty.getValue ());
  }
}
