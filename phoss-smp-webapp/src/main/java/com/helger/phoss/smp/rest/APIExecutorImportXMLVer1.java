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
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPUnauthorizedException;
import com.helger.phoss.smp.restapi.ISMPServerAPIDataProvider;
import com.helger.photon.api.IAPIDescriptor;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserManager;
import com.helger.servlet.response.UnifiedResponse;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * REST API to import Service Groups from XML v1
 *
 * @author Philip Helger
 * @since 6.0.0
 */
public final class APIExecutorImportXMLVer1 extends AbstractSMPAPIExecutor
{
  private static final boolean DEFAULT_OVERWRITE_EXISTING = false;

  public static final String PARAM_OVERVWRITE_EXISTING = "overwrite";

  private static final Logger LOGGER = LoggerFactory.getLogger (APIExecutorImportXMLVer1.class);

  public void invokeAPI (@Nonnull final IAPIDescriptor aAPIDescriptor,
                         @Nonnull @Nonempty final String sPath,
                         @Nonnull final Map <String, String> aPathVariables,
                         @Nonnull final IRequestWebScopeWithoutResponse aRequestScope,
                         @Nonnull final UnifiedResponse aUnifiedResponse) throws Exception
  {
    final String sPathUserLoginName = aPathVariables.get (SMPRestFilter.PARAM_USER_ID);

    final String sLogPrefix = "[Import-XML-V1] ";
    LOGGER.info (sLogPrefix + "Starting Import");

    // Only authenticated user may do so
    final BasicAuthClientCredentials aBasicAuth = SMPRestRequestHelper.getMandatoryAuth (aRequestScope.headers ());
    SMPUserManagerPhoton.validateUserCredentials (aBasicAuth);

    // Start action after authentication
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
    final IUserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
    final ISMPServerAPIDataProvider aDataProvider = new SMPRestDataProvider (aRequestScope, null);

    final ICommonsSet <String> aAllServiceGroupIDs = aServiceGroupMgr.getAllSMPServiceGroupIDs ();
    final ICommonsSet <String> aAllBusinessCardIDs = aBusinessCardMgr.getAllSMPBusinessCardIDs ();

    if (!aBasicAuth.getUserName ().equals (sPathUserLoginName))
    {
      throw new SMPUnauthorizedException ("URL user '" +
                                          sPathUserLoginName +
                                          "' does not match HTTP Basic Auth user name '" +
                                          aBasicAuth.getUserName () +
                                          "'",
                                          aDataProvider.getCurrentURI ());
    }

    final boolean bOverwriteExisting = aRequestScope.params ().getAsBoolean (PARAM_OVERVWRITE_EXISTING, DEFAULT_OVERWRITE_EXISTING);
    final IUser aDefaultOwner = aUserMgr.getActiveUserOfID (sPathUserLoginName);

  }
}
