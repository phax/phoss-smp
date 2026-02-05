package com.helger.phoss.smp.backend.mongodb.security;

import com.helger.collection.commons.ICommonsList;
import com.helger.photon.security.usergroup.IUserGroup;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.UUID;

public class MongoUserGroupManagerTest extends MongoBaseTest
{

  private final MongoUserGroupManager mongoUserGroupManager = new MongoUserGroupManager (null, null);

  @Test
  public void testUserGroupManagerCrud ()
  {
    mongoUserGroupManager.deleteAll ();

    IUserGroup newUserGroup = mongoUserGroupManager.createNewUserGroup ("group1", "description", Map.of ("foo", "bar"));
    String userGroupID = newUserGroup.getID ();
    mongoUserGroupManager.getUserGroupOfID (userGroupID);

    Assert.assertEquals (newUserGroup, mongoUserGroupManager.getUserGroupOfID (userGroupID));

    mongoUserGroupManager.setUserGroupData (userGroupID,"test user", "group for tests", Map.of ("foo1", "bar1"));
    IUserGroup updated = mongoUserGroupManager.getUserGroupOfID (userGroupID);
    Assert.assertEquals ("test user", updated.getName ());
    Assert.assertEquals ("group for tests", updated.getDescription ());
    Assert.assertEquals ("bar1", updated.attrs ().get ("foo1"));

    String userId = UUID.randomUUID ().toString ();

    mongoUserGroupManager.assignUserToUserGroup (userGroupID, userId);
    Assert.assertTrue (mongoUserGroupManager.isUserAssignedToUserGroup (userGroupID, userId));
    Assert.assertFalse (mongoUserGroupManager.isUserAssignedToUserGroup (userGroupID, "another uuid"));

    IUserGroup anotherGroup = mongoUserGroupManager.createNewUserGroup ("group2", null, null);
    String anotherGroupUserGroupID = newUserGroup.getID ();
    mongoUserGroupManager.assignUserToUserGroup (anotherGroupUserGroupID, userId);

    ICommonsList <String> allUserGroupIDsWithAssignedUser = mongoUserGroupManager.getAllUserGroupIDsWithAssignedUser (userId);
    Assert.assertTrue (allUserGroupIDsWithAssignedUser.contains (userGroupID));
    Assert.assertTrue (allUserGroupIDsWithAssignedUser.contains (anotherGroupUserGroupID));

    mongoUserGroupManager.unassignUserFromUserGroup (userGroupID, userId);
    Assert.assertFalse (mongoUserGroupManager.isUserAssignedToUserGroup (userGroupID, userId));
    mongoUserGroupManager.unassignUserFromAllUserGroups (userId);
    Assert.assertFalse (mongoUserGroupManager.isUserAssignedToUserGroup (anotherGroupUserGroupID, userId));

    String roleUUID = UUID.randomUUID ().toString ();
    mongoUserGroupManager.assignRoleToUserGroup (anotherGroupUserGroupID, roleUUID);
    mongoUserGroupManager.assignRoleToUserGroup (userGroupID, roleUUID);
    ICommonsList <String> allUserGroupIDsWithAssignedRole = mongoUserGroupManager.getAllUserGroupIDsWithAssignedRole (roleUUID);
    Assert.assertTrue (allUserGroupIDsWithAssignedRole.contains (userGroupID));
    Assert.assertTrue (allUserGroupIDsWithAssignedRole.contains (anotherGroupUserGroupID));

    Assert.assertFalse (mongoUserGroupManager.containsAnyUserGroupWithAssignedUserAndRole (userId, roleUUID));
    mongoUserGroupManager.assignUserToUserGroup (anotherGroupUserGroupID, userId);
    Assert.assertTrue (mongoUserGroupManager.containsAnyUserGroupWithAssignedUserAndRole (userId, roleUUID));

    mongoUserGroupManager.unassignRoleFromAllUserGroups (roleUUID);
    Assert.assertFalse (mongoUserGroupManager.containsAnyUserGroupWithAssignedUserAndRole (userId, roleUUID));
    Assert.assertTrue (mongoUserGroupManager.getAllUserGroupIDsWithAssignedRole (roleUUID).isEmpty ());

  }

}