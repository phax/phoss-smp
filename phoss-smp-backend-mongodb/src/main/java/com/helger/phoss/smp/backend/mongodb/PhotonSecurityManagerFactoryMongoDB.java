/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.mongodb;

import com.helger.phoss.smp.backend.mongodb.audit.AuditManagerMongoDB;
import com.helger.phoss.smp.backend.mongodb.security.RoleManagerMongoDB;
import com.helger.phoss.smp.backend.mongodb.security.UserGroupManagerMongoDB;
import com.helger.phoss.smp.backend.mongodb.security.UserManagerMongoDB;
import com.helger.phoss.smp.backend.mongodb.security.UserTokenManagerMongoDB;
import com.helger.photon.audit.IAuditManager;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.role.IRoleManager;
import com.helger.photon.security.token.user.IUserTokenManager;
import com.helger.photon.security.user.IUserManager;
import com.helger.photon.security.usergroup.IUserGroupManager;
import org.jspecify.annotations.NonNull;

public class PhotonSecurityManagerFactoryMongoDB implements PhotonSecurityManager.IFactory
{
  @NonNull
  public IAuditManager createAuditManager ()
  {
    return new AuditManagerMongoDB ();
  }

  @NonNull
  public IUserManager createUserMgr ()
  {
    return new UserManagerMongoDB ();
  }

  @NonNull
  public IRoleManager createRoleMgr ()
  {
    return new RoleManagerMongoDB ();
  }

  @NonNull
  public IUserGroupManager createUserGroupMgr (@NonNull final IUserManager aUserMgr, @NonNull final IRoleManager aRoleMgr)
  {
    return new UserGroupManagerMongoDB (aUserMgr, aRoleMgr);
  }

  @NonNull
  public IUserTokenManager createUserTokenMgr (@NonNull final IUserManager aUserMgr)
  {
    return new UserTokenManagerMongoDB (aUserMgr);
  }
}
