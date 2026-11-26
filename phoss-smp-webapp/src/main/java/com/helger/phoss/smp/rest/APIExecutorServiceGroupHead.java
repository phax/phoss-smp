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
import com.helger.datetime.domain.IHasLastModificationDateTime;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * Executor for <code>HEAD /{ServiceGroupId}</code>. It is only registered, if HTTP caching headers
 * are enabled. Only the existence and the last modification of the Service Group are read - the
 * response content is never created.
 *
 * @author Philip Helger
 * @since 8.9.0
 */
public final class APIExecutorServiceGroupHead extends AbstractSMPAPIExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorServiceGroupHead.class);

  @Override
  protected void invokeAPI (@NonNull final IAPIDescriptor aAPIDescriptor,
                            @NonNull @Nonempty final String sPath,
                            @NonNull final Map <String, String> aPathVariables,
                            @NonNull final IRequestWebScopeWithoutResponse aRequestScope,
                            @NonNull final PhotonUnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sPathServiceGroupID = StringHelper.trim (aPathVariables.get (SMPRestFilter.PARAM_SERVICE_GROUP_ID));
    final SMPRestDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope);

    LOGGER.info ("[SMP REST API] HEAD /" + sPathServiceGroupID);

    // Same checks as for GET
    final IParticipantIdentifier aParticipantID = SMPMetaManager.getIdentifierFactory ()
                                                                .parseParticipantIdentifier (sPathServiceGroupID);
    if (aParticipantID == null)
      throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, aDataProvider.getCurrentURI ());

    final IHasLastModificationDateTime aLastMod = SMPMetaManager.getServiceGroupMgr ()
                                                                .getSMPServiceGroupLastModification (aParticipantID);
    if (aLastMod == null)
      throw SMPNotFoundException.unknownSG (sPathServiceGroupID, aDataProvider.getCurrentURI ());

    SMPRestHttpCacheHelper.applyHeadResponse (aRequestScope,
                                              aUnifiedResponse,
                                              aDataProvider,
                                              aLastMod.getLastModificationDateTime ());
  }
}
