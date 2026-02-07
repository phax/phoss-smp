package com.helger.phoss.smp.backend.mongodb.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.helger.base.state.EChange;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.role.IRole;
import com.helger.photon.security.role.Role;

public final class RoleManagerMongoDBTest extends MongoBaseTest
{
  @Rule
  public final SMPServerTestRule m_aRule = new SMPServerTestRule ();

  @Test
  public void testRoleManagerCrud ()
  {
    try (final RoleManagerMongoDB mongoRoleManager = new RoleManagerMongoDB ())
    {
      mongoRoleManager.getCollection ().drop ();

      final IRole newRole = mongoRoleManager.createNewRole ("name", "descritpion", Map.of ("foo", "bar"));
      final String id = newRole.getID ();

      assertTrue (mongoRoleManager.containsWithID (id));

      final IRole roleOfID = mongoRoleManager.getRoleOfID (id);

      assertEquals (newRole, roleOfID);

      mongoRoleManager.renameRole (id, "renamed");
      final IRole renamed = mongoRoleManager.getRoleOfID (id);
      assertEquals ("renamed", renamed.getName ());

      final IRole getFromAll = mongoRoleManager.getAll ().get (0);
      assertEquals (renamed, getFromAll);

      assertEquals (1, mongoRoleManager.getAllActive ().size ());

      final EChange eChange = mongoRoleManager.setRoleData (id, "newName", null, null);
      assertEquals (EChange.CHANGED, eChange);

      final Role updated = (Role) mongoRoleManager.getRoleOfID (id);

      assertEquals ("newName", updated.getName ());
      assertNull (updated.getDescription ());
      assertTrue (updated.attrs ().isEmpty ());

      mongoRoleManager.deleteRole (id);
      assertEquals (0, mongoRoleManager.getAllActive ().size ());
      assertEquals (1, mongoRoleManager.getAllDeleted ().size ());
      assertEquals (1, mongoRoleManager.getAll ().size ());
    }
  }
}
