package com.helger.phoss.smp.backend.mongodb.security;

import com.helger.annotation.Nonempty;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.backend.mongodb.MongoClientSingleton;
import com.helger.photon.security.role.IRoleManager;
import com.helger.photon.security.user.IUserManager;
import com.helger.photon.security.usergroup.IUserGroup;
import com.helger.photon.security.usergroup.IUserGroupManager;
import com.helger.photon.security.usergroup.IUserGroupModificationCallback;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class MongoUserGroupManager implements IUserGroupManager
{
  public static final String GROUP_COLLECTION_NAME = "user-groups";

  private final MongoCollection <Document> m_groups;
  private final IUserManager m_aUserMgr;
  private final IRoleManager m_aRoleMgr;

  public MongoUserGroupManager (IUserManager m_aUserMgr, IRoleManager m_aRoleMgr)
  {
    this.m_aUserMgr = m_aUserMgr;
    this.m_aRoleMgr = m_aRoleMgr;
    this.m_groups = MongoClientSingleton.getInstance ().getCollection (GROUP_COLLECTION_NAME);
  }

  @Override
  public @NonNull IUserManager getUserManager ()
  {
    return null;
  }

  @Override
  public @NonNull IRoleManager getRoleManager ()
  {
    return null;
  }

  @Override
  public void createDefaultsForTest ()
  {

  }

  @Override
  public @NonNull CallbackList <IUserGroupModificationCallback> userGroupModificationCallbacks ()
  {
    return null;
  }

  @Override
  public @Nullable IUserGroup createNewUserGroup (@NonNull @Nonempty String sName, @Nullable String sDescription, @Nullable Map <String, String> aCustomAttrs)
  {
    return null;
  }

  @Override
  public @Nullable IUserGroup createPredefinedUserGroup (@NonNull @Nonempty String sID, @NonNull @Nonempty String sName, @Nullable String sDescription, @Nullable Map <String, String> aCustomAttrs)
  {
    return null;
  }

  @Override
  public @NonNull EChange deleteUserGroup (@Nullable String sUserGroupID)
  {
    return null;
  }

  @Override
  public @NonNull EChange undeleteUserGroup (@Nullable String sUserGroupID)
  {
    return null;
  }

  @Override
  public @Nullable IUserGroup getUserGroupOfID (@Nullable String sUserGroupID)
  {
    return null;
  }

  @Override
  public @NonNull ICommonsList <IUserGroup> getAllActiveUserGroups ()
  {
    return null;
  }

  @Override
  public @NonNull ICommonsList <IUserGroup> getAllDeletedUserGroups ()
  {
    return null;
  }

  @Override
  public @NonNull EChange renameUserGroup (@Nullable String sUserGroupID, @NonNull @Nonempty String sNewName)
  {
    return null;
  }

  @Override
  public @NonNull EChange setUserGroupData (@Nullable String sUserGroupID, @NonNull @Nonempty String sNewName, @Nullable String sNewDescription, @Nullable Map <String, String> aNewCustomAttrs)
  {
    return null;
  }

  @Override
  public @NonNull EChange assignUserToUserGroup (@Nullable String sUserGroupID, @NonNull @Nonempty String sUserID)
  {
    return null;
  }

  @Override
  public @NonNull EChange unassignUserFromUserGroup (@Nullable String sUserGroupID, @Nullable String sUserID)
  {
    return null;
  }

  @Override
  public @NonNull EChange unassignUserFromAllUserGroups (@Nullable String sUserID)
  {
    return null;
  }

  @Override
  public @NonNull ICommonsList <IUserGroup> getAllUserGroupsWithAssignedUser (@Nullable String sUserID)
  {
    return null;
  }

  @Override
  public @NonNull ICommonsList <String> getAllUserGroupIDsWithAssignedUser (@Nullable String sUserID)
  {
    return null;
  }

  @Override
  public @NonNull EChange assignRoleToUserGroup (@Nullable String sUserGroupID, @NonNull @Nonempty String sRoleID)
  {
    return null;
  }

  @Override
  public @NonNull EChange unassignRoleFromUserGroup (@Nullable String sUserGroupID, @Nullable String sRoleID)
  {
    return null;
  }

  @Override
  public @NonNull EChange unassignRoleFromAllUserGroups (@Nullable String sRoleID)
  {
    return null;
  }

  @Override
  public @NonNull ICommonsList <IUserGroup> getAllUserGroupsWithAssignedRole (@Nullable String sRoleID)
  {
    return null;
  }

  @Override
  public @NonNull ICommonsList <String> getAllUserGroupIDsWithAssignedRole (@Nullable String sRoleID)
  {
    return null;
  }

  @Override
  public boolean containsUserGroupWithAssignedRole (@Nullable String sRoleID)
  {
    return false;
  }

  @Override
  public boolean containsAnyUserGroupWithAssignedUserAndRole (@Nullable String sUserID, @Nullable String sRoleID)
  {
    return false;
  }

  @Override
  public @NonNull <T> ICommonsList <T> getNone ()
  {
    return null;
  }

  @Override
  public @NonNull ICommonsList <IUserGroup> getAll ()
  {
    return null;
  }

  @Override
  public boolean containsWithID (@Nullable String sID)
  {
    return false;
  }

  @Override
  public boolean containsAllIDs (@Nullable Iterable <String> aIDs)
  {
    return false;
  }
}
