/*
 * Copyright (C) 2019-2023 Philip Helger and contributors
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

import javax.annotation.Nonnull;

import com.helger.dao.DAOException;
import com.helger.phoss.smp.backend.mongodb.audit.AuditManagerMongoDB;
import com.helger.photon.audit.IAuditManager;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.role.IRoleManager;
import com.helger.photon.security.role.RoleManager;
import com.helger.photon.security.token.user.IUserTokenManager;
import com.helger.photon.security.token.user.UserTokenManager;
import com.helger.photon.security.user.IUserManager;
import com.helger.photon.security.user.UserManager;
import com.helger.photon.security.usergroup.IUserGroupManager;
import com.helger.photon.security.usergroup.UserGroupManager;

public class PhotonSecurityManagerFactoryMongoDB implements PhotonSecurityManager.IFactory
{
  @Nonnull
  public IAuditManager createAuditManager () throws Exception
  {
    return new AuditManagerMongoDB ();
  }

  @Nonnull
  public IUserManager createUserMgr () throws DAOException
  {
    return new UserManager (PhotonSecurityManager.FactoryXML.DIRECTORY_SECURITY +
                            PhotonSecurityManager.FactoryXML.FILENAME_USERS_XML);
  }

  @Nonnull
  public IRoleManager createRoleMgr () throws DAOException
  {
    return new RoleManager (PhotonSecurityManager.FactoryXML.DIRECTORY_SECURITY +
                            PhotonSecurityManager.FactoryXML.FILENAME_ROLES_XML);
  }

  @Nonnull
  public IUserGroupManager createUserGroupMgr (@Nonnull final IUserManager aUserMgr,
                                               @Nonnull final IRoleManager aRoleMgr) throws DAOException
  {
    return new UserGroupManager (PhotonSecurityManager.FactoryXML.DIRECTORY_SECURITY +
                                 PhotonSecurityManager.FactoryXML.FILENAME_USERGROUPS_XML,
                                 aUserMgr,
                                 aRoleMgr);
  }

  @Nonnull
  public IUserTokenManager createUserTokenMgr () throws DAOException
  {
    return new UserTokenManager (PhotonSecurityManager.FactoryXML.DIRECTORY_SECURITY +
                                 PhotonSecurityManager.FactoryXML.FILENAME_USERTOKENS_XML);
  }
}
