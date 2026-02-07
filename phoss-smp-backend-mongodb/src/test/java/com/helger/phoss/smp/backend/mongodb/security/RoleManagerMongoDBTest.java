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
      aRoleMgr.getCollection ().drop ();

      final IRole aRole1 = aRoleMgr.createNewRole ("name", "descritpion", Map.of ("foo", "bar"));
      assertNotNull (aRole1);
      final String sID1 = aRole1.getID ();
      assertTrue (aRoleMgr.containsWithID (sID1));

      final IRole aResolvedRole = aRoleMgr.getRoleOfID (sID1);
      assertNotNull (aResolvedRole);
      assertEquals (aRole1, aResolvedRole);

      assertTrue (aRoleMgr.renameRole (sID1, "renamed").isChanged ());
      final IRole aRenamedRole = aRoleMgr.getRoleOfID (sID1);
      assertEquals ("renamed", aRenamedRole.getName ());

      assertEquals (1, aRoleMgr.getAll ().size ());
      final IRole aFromAllRole = aRoleMgr.getAll ().get (0);
      assertEquals (aRenamedRole, aFromAllRole);

      assertEquals (1, aRoleMgr.getAllActive ().size ());

      assertTrue (aRoleMgr.setRoleData (sID1, "newName", null, null).isChanged ());

      final IRole aUpdatedRole = aRoleMgr.getRoleOfID (sID1);
      assertEquals ("newName", aUpdatedRole.getName ());
      assertNull (aUpdatedRole.getDescription ());
      assertTrue (aUpdatedRole.attrs ().isEmpty ());

      assertTrue (aRoleMgr.deleteRole (sID1).isChanged ());
      assertEquals (0, aRoleMgr.getAllActive ().size ());
      assertEquals (1, aRoleMgr.getAllDeleted ().size ());
      assertEquals (1, aRoleMgr.getAll ().size ());
    }
  }
}
