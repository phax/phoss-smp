package com.helger.phoss.smp.backend.mongodb.security;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.helger.base.state.EChange;
import com.helger.photon.security.role.IRole;
import com.helger.photon.security.role.Role;

public class MongoRoleManagerTest extends MongoBaseTest
{

  private final MongoRoleManager mongoRoleManager = new MongoRoleManager ();

  @Test
  public void testRoleManagerCrud ()
  {
    mongoRoleManager.getCollection ().drop ();

    final IRole newRole = mongoRoleManager.createNewRole ("name", "descritpion", Map.of ("foo", "bar"));
    final String id = newRole.getID ();

    Assert.assertTrue (mongoRoleManager.containsWithID (id));

    final IRole roleOfID = mongoRoleManager.getRoleOfID (id);

    Assert.assertEquals (newRole, roleOfID);

    mongoRoleManager.renameRole (id, "renamed");
    final IRole renamed = mongoRoleManager.getRoleOfID (id);
    Assert.assertEquals ("renamed", renamed.getName ());

    final IRole getFromAll = mongoRoleManager.getAll ().get (0);
    Assert.assertEquals (renamed, getFromAll);

    Assert.assertEquals (1, mongoRoleManager.getAllActive ().size ());

    final EChange eChange = mongoRoleManager.setRoleData (id, "newName", null, null);
    Assert.assertEquals (EChange.CHANGED, eChange);

    final Role updated = (Role) mongoRoleManager.getRoleOfID (id);

    Assert.assertEquals ("newName", updated.getName ());
    Assert.assertNull (updated.getDescription ());
    Assert.assertTrue (updated.attrs ().isEmpty ());

    mongoRoleManager.deleteRole (id);
    Assert.assertEquals (0, mongoRoleManager.getAllActive ().size ());
    Assert.assertEquals (1, mongoRoleManager.getAllDeleted ().size ());
    Assert.assertEquals (1, mongoRoleManager.getAll ().size ());
  }

}
