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
package com.helger.phoss.smp.backend.mongodb.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.role.IRole;

public final class RoleManagerMongoDBTest
{
  @Rule
  public final SMPServerTestRule m_aRule = new SMPServerTestRule ();

  @Test
  public void testRoleManagerCrud ()
  {
    try (final RoleManagerMongoDB aRoleMgr = new RoleManagerMongoDB ())
    {
      final IRole aRole = aRoleMgr.createNewRole ("name", "description", Map.of ("foo", "bar"));
      assertNotNull (aRole);
      final String sRoleID = aRole.getID ();

      try
      {
        assertTrue (aRoleMgr.containsWithID (sRoleID));

        final IRole aResolvedRole = aRoleMgr.getRoleOfID (sRoleID);
        assertNotNull (aResolvedRole);
        assertEquals (aRole, aResolvedRole);

        assertTrue (aRoleMgr.renameRole (sRoleID, "renamed").isChanged ());
        final IRole aRenamedRole = aRoleMgr.getRoleOfID (sRoleID);
        assertEquals ("renamed", aRenamedRole.getName ());

        assertTrue (aRoleMgr.getAll ().contains (aRole));
        assertTrue (aRoleMgr.getAllActive ().contains (aRole));

        assertTrue (aRoleMgr.setRoleData (sRoleID, "newName", null, null).isChanged ());

        final IRole aUpdatedRole = aRoleMgr.getRoleOfID (sRoleID);
        assertEquals ("newName", aUpdatedRole.getName ());
        assertNull (aUpdatedRole.getDescription ());
        assertTrue (aUpdatedRole.attrs ().isEmpty ());
      }
      finally
      {
        aRoleMgr.internalDeleteRoleNotRecoverable (sRoleID);
      }
    }
  }
}
