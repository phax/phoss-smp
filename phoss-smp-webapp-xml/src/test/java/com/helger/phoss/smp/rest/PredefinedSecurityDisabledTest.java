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

import static org.junit.Assert.assertFalse;

import org.junit.Rule;
import org.junit.Test;

import com.helger.io.resource.FileSystemResource;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.role.IRoleManager;
import com.helger.photon.security.user.IUserManager;
import com.helger.photon.security.usergroup.IUserGroupManager;

/**
 * Test that the predefined users, user groups and roles are not created, if the configuration
 * property <code>smp.security.predefined.create</code> is <code>false</code>.
 *
 * @author Philip Helger
 */
public final class PredefinedSecurityDisabledTest
{
  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (new FileSystemResource ("src/test/resources/test-smp-server-xml-no-predefined-security.properties"));

  @Test
  public void testNoPredefinedObjects ()
  {
    final IUserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
    assertFalse (aUserMgr.containsWithID (CSMP.USER_ADMINISTRATOR_ID));

    final IUserGroupManager aUserGroupMgr = PhotonSecurityManager.getUserGroupMgr ();
    assertFalse (aUserGroupMgr.containsWithID (CSMP.USERGROUP_ADMINISTRATORS_ID));
    assertFalse (aUserGroupMgr.containsWithID (CSMP.USERGROUP_CONFIG_ID));
    assertFalse (aUserGroupMgr.containsWithID (CSMP.USERGROUP_WRITABLERESTAPI_ID));

    final IRoleManager aRoleMgr = PhotonSecurityManager.getRoleMgr ();
    assertFalse (aRoleMgr.containsWithID (CSMP.ROLE_CONFIG_ID));
    assertFalse (aRoleMgr.containsWithID (CSMP.ROLE_WRITABLERESTAPI_ID));
  }
}
