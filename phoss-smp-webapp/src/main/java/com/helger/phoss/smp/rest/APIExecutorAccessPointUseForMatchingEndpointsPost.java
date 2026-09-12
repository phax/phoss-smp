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
import com.helger.base.string.StringHelper;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.restapi.AccessPointServerAPI;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.phoss.smp.restapi.SMPAPICredentials;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.MicroDocument;

/**
 * REST API executor for
 * <code>POST /accesspoint/name/{AccessPointName}/use-for-matching-endpoints</code>. Authenticated,
 * administrators only. Lets all endpoints with the same certificate use the referenced Access Point
 * instead of their direct data. This is the explicit alternative to an automatic data migration.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public final class APIExecutorAccessPointUseForMatchingEndpointsPost extends AbstractSMPAPIExecutor
{
  public static final String PARAM_REQUIRE_SAME_URL = "require-same-url";
  public static final String ELEMENT_RESULT = "accesspointadoption";
  public static final String ATTR_CHANGED_ENDPOINTS = "changedendpoints";

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
      throw new SMPPreconditionFailedException ("The writable REST API is disabled. accesspoint POST will not be executed",
                                                aDataProvider.getCurrentURI ());
    }

    // Authenticate
    final SMPAPICredentials aCredentials = getMandatoryAuth (aRequestScope.headers ());

    // By default only endpoints with the same URL are changed, because that is lossless
    final boolean bRequireSameURL = aRequestScope.params ().getAsBoolean (PARAM_REQUIRE_SAME_URL, true);

    final long nChangedEndpoints = new AccessPointServerAPI (aDataProvider).useAccessPointForMatchingEndpoints (sAccessPointName,
                                                                                                                bRequireSameURL,
                                                                                                                aCredentials);

    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement eRoot = aDoc.addElement (ELEMENT_RESULT);
    eRoot.setAttribute (ATTR_CHANGED_ENDPOINTS, nChangedEndpoints);

    aUnifiedResponse.xml (aDoc).disableCaching ();
  }
}
