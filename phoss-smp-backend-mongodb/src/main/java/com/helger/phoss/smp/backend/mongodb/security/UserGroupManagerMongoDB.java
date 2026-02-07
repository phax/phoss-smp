package com.helger.phoss.smp.backend.mongodb.security;

import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.misc.DevelopersNote;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.callback.CallbackList;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.security.role.IRoleManager;
import com.helger.photon.security.user.IUserManager;
import com.helger.photon.security.usergroup.IUserGroup;
import com.helger.photon.security.usergroup.IUserGroupManager;
import com.helger.photon.security.usergroup.IUserGroupModificationCallback;
import com.helger.photon.security.usergroup.UserGroup;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

public class UserGroupManagerMongoDB extends AbstractBusinessObjectManagerMongoDB <IUserGroup, UserGroup> implements
                                     IUserGroupManager
{
  public static final String GROUP_COLLECTION_NAME = "user-groups";

  private static final String BSON_USER_GROUP_NAME = "name";
  private static final String BSON_USER_GROUP_DESCRIPTION = "description";
  private static final String BSON_USER_GROUP_ROLES = "roleIDs";
  private static final String BSON_USER_GROUP_USERS = "userIDs";

  private final IUserManager m_aUserMgr;
  private final IRoleManager m_aRoleMgr;

  private final CallbackList <IUserGroupModificationCallback> m_aCallbacks = new CallbackList <> ();

  public UserGroupManagerMongoDB (@NonNull final IUserManager aUserMgr, @NonNull final IRoleManager aRoleMgr)
  {
    super (GROUP_COLLECTION_NAME);
    ValueEnforcer.notNull (aUserMgr, "UserMgr");
    ValueEnforcer.notNull (aRoleMgr, "RoleMgr");
    m_aUserMgr = aUserMgr;
    m_aRoleMgr = aRoleMgr;
  }

  @Override
  public final @NonNull IUserManager getUserManager ()
  {
    return m_aUserMgr;
  }

  @Override
  public final @NonNull IRoleManager getRoleManager ()
  {
    return m_aRoleMgr;
  }

  @Override
  protected @NonNull Document toBson (@NonNull final IUserGroup aUserGroup)
  {
    return getDefaultBusinessDocument (aUserGroup).append (BSON_USER_GROUP_NAME, aUserGroup.getName ())
                                                  .append (BSON_USER_GROUP_DESCRIPTION, aUserGroup.getDescription ())
                                                  .append (BSON_USER_GROUP_ROLES, aUserGroup.getAllContainedRoleIDs ())
                                                  .append (BSON_USER_GROUP_USERS, aUserGroup.getAllContainedUserIDs ());
  }

  @Override
  protected @NonNull UserGroup toEntity (@NonNull final Document aDoc)
  {
    final UserGroup ret = new UserGroup (populateStubObject (aDoc),
                                         aDoc.getString (BSON_USER_GROUP_NAME),
                                         aDoc.getString (BSON_USER_GROUP_DESCRIPTION));

    final List <String> aRoles = aDoc.getList (BSON_USER_GROUP_ROLES, String.class);
    ret.assignRoles (aRoles);

    final List <String> aUsers = aDoc.getList (BSON_USER_GROUP_USERS, String.class);
    ret.assignUsers (aUsers);

    return ret;
  }

  @Override
  public void createDefaultsForTest ()
  {
    // ignored for now
  }

  @Override
  @ReturnsMutableCopy
  public @NonNull CallbackList <IUserGroupModificationCallback> userGroupModificationCallbacks ()
  {
    return m_aCallbacks;
  }

  @Nullable
  private UserGroup _internalCreateNewUserGroup (@NonNull final UserGroup aUserGroup, final boolean bPredefined)
  {
    if (!getCollection ().insertOne (toBson (aUserGroup)).wasAcknowledged ())
    {
      AuditHelper.onAuditCreateFailure (UserGroup.OT,
                                        aUserGroup.getID (),
                                        aUserGroup.getName (),
                                        aUserGroup.getDescription (),
                                        aUserGroup.attrs (),
                                        bPredefined ? "predefined" : "custom",
                                        "database-error");
      return null;
    }

    AuditHelper.onAuditCreateSuccess (UserGroup.OT,
                                      aUserGroup.getID (),
                                      aUserGroup.getName (),
                                      aUserGroup.getDescription (),
                                      aUserGroup.attrs (),
                                      bPredefined ? "predefined" : "custom");

    m_aCallbacks.forEach (aCB -> aCB.onUserGroupCreated (aUserGroup, bPredefined));

    return aUserGroup;
  }

  @Override
  public @Nullable IUserGroup createNewUserGroup (@NonNull @Nonempty final String sName,
                                                  @Nullable final String sDescription,
                                                  @Nullable final Map <String, String> aCustomAttrs)
  {
    final UserGroup aUserGroup = new UserGroup (sName, sDescription, aCustomAttrs);
    return _internalCreateNewUserGroup (aUserGroup, false);
  }

  @Override
  public @Nullable IUserGroup createPredefinedUserGroup (@NonNull @Nonempty final String sID,
                                                         @NonNull @Nonempty final String sName,
                                                         @Nullable final String sDescription,
                                                         @Nullable final Map <String, String> aCustomAttrs)
  {
    final UserGroup aUserGroup = new UserGroup (sName, sDescription, aCustomAttrs);
    return _internalCreateNewUserGroup (aUserGroup, true);
  }

  @DevelopersNote ("For internal use only")
  public @Nullable IUserGroup internalCreateMigrationUserGroup (@NonNull final UserGroup aSrcUserGroup)
  {
    // Create UserGroup
    return _internalCreateNewUserGroup (aSrcUserGroup, true);
  }

  @Override
  public @NonNull EChange deleteUserGroup (@Nullable final String sUserGroupID)
  {
    if (StringHelper.isEmpty (sUserGroupID))
      return EChange.UNCHANGED;

    final EChange eChange = deleteEntity (sUserGroupID);
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditDeleteSuccess (UserGroup.OT, sUserGroupID);

      m_aCallbacks.forEach (aCB -> aCB.onUserGroupDeleted (sUserGroupID));
    }
    else
    {
      AuditHelper.onAuditDeleteFailure (UserGroup.OT, sUserGroupID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange undeleteUserGroup (@Nullable final String sUserGroupID)
  {
    if (StringHelper.isEmpty (sUserGroupID))
      return EChange.UNCHANGED;

    final EChange eChange = undeleteEntity (sUserGroupID);
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditUndeleteSuccess (UserGroup.OT, sUserGroupID);

      m_aCallbacks.forEach (aCB -> aCB.onUserGroupUndeleted (sUserGroupID));
    }
    else
    {
      AuditHelper.onAuditUndeleteFailure (UserGroup.OT, sUserGroupID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @Nullable IUserGroup getUserGroupOfID (@Nullable final String sUserGroupID)
  {
    return findByID (sUserGroupID);
  }

  @Override
  public @NonNull ICommonsList <IUserGroup> getAllActiveUserGroups ()
  {
    return getAllActive ();
  }

  @Override
  public @NonNull ICommonsList <IUserGroup> getAllDeletedUserGroups ()
  {
    return getAllDeleted ();
  }

  @Override
  public @NonNull EChange renameUserGroup (@Nullable final String sUserGroupID,
                                           @NonNull @Nonempty final String sNewName)
  {
    if (StringHelper.isEmpty (sUserGroupID))
      return EChange.UNCHANGED;

    final EChange eChange = genericUpdateOne (sUserGroupID,
                                              addLastModToUpdate (Updates.set (BSON_USER_GROUP_NAME, sNewName)));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (UserGroup.OT, "set-name", sUserGroupID, sNewName);

      m_aCallbacks.forEach (aCB -> aCB.onUserGroupRenamed (sUserGroupID));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "set-name", sUserGroupID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange setUserGroupData (@Nullable final String sUserGroupID,
                                            @NonNull @Nonempty final String sNewName,
                                            @Nullable final String sNewDescription,
                                            @Nullable final Map <String, String> aNewCustomAttrs)
  {
    if (StringHelper.isEmpty (sUserGroupID))
      return EChange.UNCHANGED;

    final EChange eChange = genericUpdateOne (sUserGroupID,
                                              addLastModToUpdate (Updates.combine (Updates.set (BSON_USER_GROUP_NAME,
                                                                                                sNewName),
                                                                                   Updates.set (BSON_USER_GROUP_DESCRIPTION,
                                                                                                sNewDescription),
                                                                                   Updates.set (BSON_ATTRIBUTES,
                                                                                                aNewCustomAttrs))));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (UserGroup.OT,
                                        "set-all",
                                        sUserGroupID,
                                        sNewName,
                                        sNewDescription,
                                        aNewCustomAttrs);

      m_aCallbacks.forEach (aCB -> aCB.onUserGroupUpdated (sUserGroupID));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "set-all", sUserGroupID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange assignUserToUserGroup (@Nullable final String sUserGroupID,
                                                 @NonNull @Nonempty final String sUserID)
  {
    if (StringHelper.isEmpty (sUserGroupID))
      return EChange.UNCHANGED;

    final EChange eChange = genericUpdateOne (sUserGroupID,
                                              addLastModToUpdate (Updates.push (BSON_USER_GROUP_USERS, sUserID)));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (UserGroup.OT, "assign-user", sUserGroupID, sUserID);

      m_aCallbacks.forEach (aCB -> aCB.onUserGroupUserAssignment (sUserGroupID, sUserID, true));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "assign-user", sUserGroupID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange unassignUserFromUserGroup (@Nullable final String sUserGroupID,
                                                     @Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserGroupID))
      return EChange.UNCHANGED;

    final EChange eChange = genericUpdateOne (sUserGroupID,
                                              addLastModToUpdate (Updates.pull (BSON_USER_GROUP_USERS, sUserID)));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (UserGroup.OT, "unassign-user", sUserGroupID, sUserID);

      m_aCallbacks.forEach (aCB -> aCB.onUserGroupUserAssignment (sUserGroupID, sUserID, false));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "unassign-user", sUserGroupID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange unassignUserFromAllUserGroups (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    final ICommonsList <String> aAllUGs = getAllUserGroupIDsWithAssignedUser (sUserID);
    int nUnassigned = 0;
    for (final String sUGID : aAllUGs)
    {
      if (unassignUserFromUserGroup (sUGID, sUserID).isChanged ())
        nUnassigned++;
    }

    if (nUnassigned > 0)
    {
      AuditHelper.onAuditModifySuccess (UserGroup.OT, "unassign-user-from-all-usergroups", sUserID);
      return EChange.CHANGED;
    }

    AuditHelper.onAuditModifyFailure (UserGroup.OT, "unassign-user-from-all-usergroups", sUserID, "not-assigned");
    return EChange.UNCHANGED;
  }

  @Override
  public @NonNull ICommonsList <IUserGroup> getAllUserGroupsWithAssignedUser (@Nullable final String sUserID)
  {
    return findAll (Filters.eq (BSON_USER_GROUP_USERS, sUserID));
  }

  @Override
  public @NonNull ICommonsList <String> getAllUserGroupIDsWithAssignedUser (@Nullable final String sUserID)
  {
    return findAllIDs (Filters.eq (BSON_USER_GROUP_USERS, sUserID));
  }

  @Override
  public @NonNull EChange assignRoleToUserGroup (@Nullable final String sUserGroupID,
                                                 @NonNull @Nonempty final String sRoleID)
  {
    if (StringHelper.isEmpty (sUserGroupID))
      return EChange.UNCHANGED;

    final EChange eChange = genericUpdateOne (sUserGroupID,
                                              addLastModToUpdate (Updates.push (BSON_USER_GROUP_ROLES, sRoleID)));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (UserGroup.OT, "assign-role", sUserGroupID, sRoleID);

      m_aCallbacks.forEach (aCB -> aCB.onUserGroupRoleAssignment (sUserGroupID, sRoleID, true));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "assign-role", sUserGroupID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange unassignRoleFromUserGroup (@Nullable final String sUserGroupID,
                                                     @Nullable final String sRoleID)
  {
    if (StringHelper.isEmpty (sUserGroupID) || StringHelper.isEmpty (sRoleID))
      return EChange.UNCHANGED;

    final EChange eChange = genericUpdateOne (sUserGroupID,
                                              addLastModToUpdate (Updates.pull (BSON_USER_GROUP_ROLES, sRoleID)));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (UserGroup.OT, "unassign-role", sUserGroupID, sRoleID);

      m_aCallbacks.forEach (aCB -> aCB.onUserGroupRoleAssignment (sUserGroupID, sRoleID, false));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "unassign-role", sUserGroupID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange unassignRoleFromAllUserGroups (@Nullable final String sRoleID)
  {
    if (StringHelper.isEmpty (sRoleID))
      return EChange.UNCHANGED;

    final ICommonsList <String> aAllUGs = getAllUserGroupIDsWithAssignedRole (sRoleID);
    int nUnassigned = 0;
    for (final String sUGID : aAllUGs)
    {
      if (unassignRoleFromUserGroup (sUGID, sRoleID).isChanged ())
        nUnassigned++;
    }

    if (nUnassigned > 0)
    {
      AuditHelper.onAuditModifySuccess (UserGroup.OT, "unassign-role-from-all-usergroups", sRoleID);
      return EChange.CHANGED;
    }

    AuditHelper.onAuditModifyFailure (UserGroup.OT, "unassign-role-from-all-usergroups", sRoleID, "not-assigned");
    return EChange.UNCHANGED;
  }

  @Override
  public @NonNull ICommonsList <IUserGroup> getAllUserGroupsWithAssignedRole (@Nullable final String sRoleID)
  {
    return findAll (Filters.eq (BSON_USER_GROUP_ROLES, sRoleID));
  }

  @Override
  public @NonNull ICommonsList <String> getAllUserGroupIDsWithAssignedRole (@Nullable final String sRoleID)
  {
    return findAllIDs (Filters.eq (BSON_USER_GROUP_ROLES, sRoleID));
  }

  @Override
  public boolean containsUserGroupWithAssignedRole (@Nullable final String sRoleID)
  {
    return getCollection ().find (Filters.eq (BSON_USER_GROUP_ROLES, sRoleID)).first () != null;
  }

  @Override
  public boolean containsAnyUserGroupWithAssignedUserAndRole (@Nullable final String sUserID,
                                                              @Nullable final String sRoleID)
  {
    return getCollection ().find (Filters.and (Filters.eq (BSON_USER_GROUP_USERS, sUserID),
                                               Filters.eq (BSON_USER_GROUP_ROLES, sRoleID))).first () != null;
  }
}
