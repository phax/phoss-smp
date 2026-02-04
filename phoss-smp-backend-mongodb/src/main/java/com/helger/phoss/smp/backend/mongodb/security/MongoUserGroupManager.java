package com.helger.phoss.smp.backend.mongodb.security;

import com.helger.annotation.Nonempty;
import com.helger.base.callback.CallbackList;
import com.helger.base.id.IHasID;
import com.helger.base.state.EChange;
import com.helger.collection.commons.CommonsArrayList;
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
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class MongoUserGroupManager extends AbstractMongoManager <IUserGroup> implements IUserGroupManager
{
  public static final String GROUP_COLLECTION_NAME = "user-groups";

  private static final String BSON_USER_GROUP_NAME = "name";
  private static final String BSON_USER_GROUP_DESCRIPTION = "description";
  private static final String BSON_USER_GROUP_ROLES = "userIDs";
  private static final String BSON_USER_GROUP_USERS = "roleIDs";

  private final IUserManager m_aUserMgr;
  private final IRoleManager m_aRoleMgr;

  private final CallbackList <IUserGroupModificationCallback> m_aCallbacks = new CallbackList <> ();

  public MongoUserGroupManager (IUserManager m_aUserMgr, IRoleManager m_aRoleMgr)
  {
    super (GROUP_COLLECTION_NAME);
    this.m_aUserMgr = m_aUserMgr;
    this.m_aRoleMgr = m_aRoleMgr;
  }

  @Override
  public @NonNull IUserManager getUserManager ()
  {
    return m_aUserMgr;
  }

  @Override
  public @NonNull IRoleManager getRoleManager ()
  {
    return m_aRoleMgr;
  }


  @Override
  protected @NonNull Document toBson (@NonNull IUserGroup aUserGroup)
  {
    return getDefaultBusinessDocument (aUserGroup)
                               .append (BSON_USER_GROUP_NAME, aUserGroup.getName ())
                               .append (BSON_USER_GROUP_DESCRIPTION, aUserGroup.getDescription ())
                               .append (BSON_USER_GROUP_ROLES, aUserGroup.getAllContainedRoleIDs ())
                               .append (BSON_USER_GROUP_USERS, aUserGroup.getAllContainedUserIDs ());
  }

  @Override
  protected @NonNull IUserGroup toEntity (@NonNull Document aDoc)
  {
    UserGroup userGroup = new UserGroup (populateStubObject (aDoc),
                               aDoc.getString (BSON_USER_GROUP_NAME),
                               aDoc.getString (BSON_USER_GROUP_DESCRIPTION)
    );

    List <String> roles = aDoc.getList (BSON_USER_GROUP_ROLES, String.class);
    userGroup.assignRoles (roles);

    List <String> users = aDoc.getList (BSON_USER_GROUP_USERS, String.class);
    userGroup.assignUsers (users);

    return userGroup;
  }

  @Override
  public void createDefaultsForTest ()
  {
    //ignored for now
  }

  @Override
  public @NonNull CallbackList <IUserGroupModificationCallback> userGroupModificationCallbacks ()
  {
    return m_aCallbacks;
  }

  @Override
  public @Nullable IUserGroup createNewUserGroup (@NonNull @Nonempty String sName, @Nullable String sDescription, @Nullable Map <String, String> aCustomAttrs)
  {
    final UserGroup aUserGroup = new UserGroup (sName, sDescription, aCustomAttrs);
    return internalCreateNewUserGroup (aUserGroup, false);
  }

  @Override
  public @Nullable IUserGroup createPredefinedUserGroup (@NonNull @Nonempty String sID, @NonNull @Nonempty String sName, @Nullable String sDescription, @Nullable Map <String, String> aCustomAttrs)
  {
    final UserGroup aUserGroup = new UserGroup (sName, sDescription, aCustomAttrs);
    return internalCreateNewUserGroup (aUserGroup, true);
  }

  protected UserGroup internalCreateNewUserGroup (@NonNull final UserGroup aUserGroup, final boolean bPredefined)
  {
    try
    {
      getCollection ().insertOne (toBson (aUserGroup));
      m_aCallbacks.forEach (aCB -> aCB.onUserGroupCreated (aUserGroup, bPredefined));
      return aUserGroup;
    } catch (Exception e)
    {
      AuditHelper.onAuditCreateFailure (UserGroup.OT,
                                 aUserGroup.getID (),
                                 aUserGroup.getName (),
                                 aUserGroup.getDescription (),
                                 aUserGroup.attrs (),
                                 bPredefined ? "predefined" : "custom",
                                 "database-error");
    }
    return null;
  }


  @Override
  public @NonNull EChange deleteUserGroup (@Nullable String sUserGroupID)
  {
    return deleteEntity (sUserGroupID, () -> m_aCallbacks.forEach (aCB -> aCB.onUserGroupDeleted (sUserGroupID)));
  }

  @Override
  public @NonNull EChange undeleteUserGroup (@Nullable String sUserGroupID)
  {
    return undeleteEntity (sUserGroupID, () -> m_aCallbacks.forEach (aCB -> aCB.onUserGroupUndeleted (sUserGroupID)));
  }

  @Override
  public @Nullable IUserGroup getUserGroupOfID (@Nullable String sUserGroupID)
  {
    return findById (sUserGroupID);
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
  public @NonNull EChange renameUserGroup (@Nullable String sUserGroupID, @NonNull @Nonempty String sNewName)
  {
    return genericUpdate (sUserGroupID, Updates.set (BSON_USER_GROUP_NAME, sNewName), true, () ->
                               m_aCallbacks.forEach (aCB -> aCB.onUserGroupRenamed (sUserGroupID)));
  }

  @Override
  public @NonNull EChange setUserGroupData (@Nullable String sUserGroupID,
                                            @NonNull @Nonempty String sNewName,
                                            @Nullable String sNewDescription,
                                            @Nullable Map <String, String> aNewCustomAttrs)
  {
    return genericUpdate (sUserGroupID, Updates.combine (
                                                          Updates.set (BSON_USER_GROUP_NAME, sNewName),
                                                          Updates.set (BSON_USER_GROUP_DESCRIPTION, sNewDescription),
                                                          Updates.set (BSON_ATTRIBUTES, aNewCustomAttrs)),
                               true,
                               () -> m_aCallbacks.forEach (aCB -> aCB.onUserGroupUpdated (sUserGroupID))
    );
  }

  @Override
  public @NonNull EChange assignUserToUserGroup (@Nullable String sUserGroupID, @NonNull @Nonempty String sUserID)
  {
    return genericUpdate (sUserGroupID, Updates.push (BSON_USER_GROUP_USERS, sUserID), true, () ->
                               m_aCallbacks.forEach (aCB -> aCB.onUserGroupUserAssignment (sUserGroupID, sUserID, true)));
  }

  @Override
  public @NonNull EChange unassignUserFromUserGroup (@Nullable String sUserGroupID, @Nullable String sUserID)
  {
    return genericUpdate (sUserGroupID, Updates.pull (BSON_USER_GROUP_USERS, sUserID), true, () ->
                               m_aCallbacks.forEach (aCB -> aCB.onUserGroupUserAssignment (sUserGroupID, sUserID, false)));
  }

  @Override
  public @NonNull EChange unassignUserFromAllUserGroups (@Nullable String sUserID)
  {
    UpdateResult updateResult = getCollection ().updateMany (new Document (), addLastModToUpdate (Updates.pull (BSON_USER_GROUP_USERS, sUserID)));
    if (updateResult.getMatchedCount () > 0)
    {
      m_aCallbacks.forEach (aCB -> aCB.onUserGroupUserAssignment (null, sUserID, false)); //not supported in mongodb
      return EChange.CHANGED;
    }
    return EChange.UNCHANGED;
  }

  @Override
  public @NonNull ICommonsList <IUserGroup> getAllUserGroupsWithAssignedUser (@Nullable String sUserID)
  {
    ICommonsList <IUserGroup> aList = new CommonsArrayList <> ();
    getCollection ().find (Filters.eq (BSON_USER_GROUP_USERS, sUserID)).forEach (document -> aList.add (toEntity (document)));
    return aList;
  }

  @Override
  public @NonNull ICommonsList <String> getAllUserGroupIDsWithAssignedUser (@Nullable String sUserID)
  {
    return getAllUserGroupsWithAssignedUser (sUserID).getAllMapped (IHasID::getID);
  }

  @Override
  public @NonNull EChange assignRoleToUserGroup (@Nullable String sUserGroupID, @NonNull @Nonempty String sRoleID)
  {
    return genericUpdate (sUserGroupID, Updates.push (BSON_USER_GROUP_ROLES, sRoleID), true, () ->
                               m_aCallbacks.forEach (aCB -> aCB.onUserGroupRoleAssignment (sUserGroupID, sRoleID, true)));
  }

  @Override
  public @NonNull EChange unassignRoleFromUserGroup (@Nullable String sUserGroupID, @Nullable String sRoleID)
  {
    return genericUpdate (sUserGroupID, Updates.pull (BSON_USER_GROUP_ROLES, sRoleID), true, () ->
                               m_aCallbacks.forEach (aCB -> aCB.onUserGroupRoleAssignment (sUserGroupID, sRoleID, false)));
  }

  @Override
  public @NonNull EChange unassignRoleFromAllUserGroups (@Nullable String sRoleID)
  {
    return getCollection ().updateMany (new Document (), addLastModToUpdate (Updates.pull (BSON_USER_GROUP_ROLES, sRoleID)))
                               .getMatchedCount () > 0 ? EChange.CHANGED : EChange.UNCHANGED;
  }

  @Override
  public @NonNull ICommonsList <IUserGroup> getAllUserGroupsWithAssignedRole (@Nullable String sRoleID)
  {
    ICommonsList <IUserGroup> aList = new CommonsArrayList <> ();
    getCollection ().find (Filters.eq (BSON_USER_GROUP_ROLES, sRoleID)).forEach (document -> aList.add (toEntity (document)));
    return aList;
  }

  @Override
  public @NonNull ICommonsList <String> getAllUserGroupIDsWithAssignedRole (@Nullable String sRoleID)
  {
    return getAllUserGroupsWithAssignedRole (sRoleID).getAllMapped (IHasID::getID);
  }

  @Override
  public boolean containsUserGroupWithAssignedRole (@Nullable String sRoleID)
  {
    return getCollection ().countDocuments (Filters.eq (BSON_USER_GROUP_ROLES, sRoleID)) > 0;
  }

  @Override
  public boolean containsAnyUserGroupWithAssignedUserAndRole (@Nullable String sUserID, @Nullable String sRoleID)
  {
    return getCollection ().countDocuments (Filters.and (
                               Filters.eq (BSON_USER_GROUP_USERS, sUserID),
                               Filters.eq (BSON_USER_GROUP_ROLES, sRoleID)
    )) > 0;
  }

}
