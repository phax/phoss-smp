/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.app;

import javax.annotation.concurrent.Immutable;

import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.role.RoleManager;
import com.helger.photon.security.user.UserManager;
import com.helger.photon.security.usergroup.UserGroupManager;

@Immutable
public final class AppSecurity
{
  private AppSecurity ()
  {}

  public static void init ()
  {
    final UserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
    final UserGroupManager aUserGroupMgr = PhotonSecurityManager.getUserGroupMgr ();
    final RoleManager aRoleMgr = PhotonSecurityManager.getRoleMgr ();

    // Standard users
    if (!aUserMgr.containsUserWithID (CApp.USER_ADMINISTRATOR_ID))
    {
      final boolean bDisabled = false;
      aUserMgr.createPredefinedUser (CApp.USER_ADMINISTRATOR_ID,
                                     CApp.USER_ADMINISTRATOR_LOGINNAME,
                                     CApp.USER_ADMINISTRATOR_EMAIL,
                                     CApp.USER_ADMINISTRATOR_PASSWORD,
                                     CApp.USER_ADMINISTRATOR_FIRSTNAME,
                                     CApp.USER_ADMINISTRATOR_LASTNAME,
                                     CApp.USER_ADMINISTRATOR_DESCRIPTION,
                                     CApp.USER_ADMINISTRATOR_LOCALE,
                                     CApp.USER_ADMINISTRATOR_CUSTOMATTRS,
                                     bDisabled);
    }

    // Create all roles
    if (!aRoleMgr.containsRoleWithID (CApp.ROLE_CONFIG_ID))
      aRoleMgr.createPredefinedRole (CApp.ROLE_CONFIG_ID, CApp.ROLE_CONFIG_NAME, CApp.ROLE_CONFIG_DESCRIPTION, CApp.ROLE_CONFIG_CUSTOMATTRS);
    if (!aRoleMgr.containsRoleWithID (CApp.ROLE_VIEW_ID))
      aRoleMgr.createPredefinedRole (CApp.ROLE_VIEW_ID, CApp.ROLE_VIEW_NAME, CApp.ROLE_VIEW_DESCRIPTION, CApp.ROLE_VIEW_CUSTOMATTRS);

    // User group Administrators
    if (!aUserGroupMgr.containsUserGroupWithID (CApp.USERGROUP_ADMINISTRATORS_ID))
    {
      aUserGroupMgr.createPredefinedUserGroup (CApp.USERGROUP_ADMINISTRATORS_ID,
                                               CApp.USERGROUP_ADMINISTRATORS_NAME,
                                               CApp.USERGROUP_ADMINISTRATORS_DESCRIPTION,
                                               CApp.USERGROUP_ADMINISTRATORS_CUSTOMATTRS);
      // Assign administrator user to administrators user group
      aUserGroupMgr.assignUserToUserGroup (CApp.USERGROUP_ADMINISTRATORS_ID, CApp.USER_ADMINISTRATOR_ID);
    }
    aUserGroupMgr.assignRoleToUserGroup (CApp.USERGROUP_ADMINISTRATORS_ID, CApp.ROLE_CONFIG_ID);
    aUserGroupMgr.assignRoleToUserGroup (CApp.USERGROUP_ADMINISTRATORS_ID, CApp.ROLE_VIEW_ID);

    // User group for Config users
    if (!aUserGroupMgr.containsUserGroupWithID (CApp.USERGROUP_CONFIG_ID))
      aUserGroupMgr.createPredefinedUserGroup (CApp.USERGROUP_CONFIG_ID,
                                               CApp.USERGROUP_CONFIG_NAME,
                                               CApp.USERGROUP_CONFIG_DESCRIPTION,
                                               CApp.USERGROUP_CONFIG_CUSTOMATTRS);
    aUserGroupMgr.assignRoleToUserGroup (CApp.USERGROUP_CONFIG_ID, CApp.ROLE_CONFIG_ID);

    // User group for View users
    if (!aUserGroupMgr.containsUserGroupWithID (CApp.USERGROUP_VIEW_ID))
      aUserGroupMgr.createPredefinedUserGroup (CApp.USERGROUP_VIEW_ID,
                                               CApp.USERGROUP_VIEW_NAME,
                                               CApp.USERGROUP_VIEW_DESCRIPTION,
                                               CApp.USERGROUP_VIEW_CUSTOMATTRS);
    aUserGroupMgr.assignRoleToUserGroup (CApp.USERGROUP_VIEW_ID, CApp.ROLE_VIEW_ID);
  }
}
