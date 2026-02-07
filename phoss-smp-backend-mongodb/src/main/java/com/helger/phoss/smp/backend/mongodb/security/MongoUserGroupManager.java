package com.helger.phoss.smp.backend.mongodb.security;

import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.callback.CallbackList;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.id.IHasID;
import com.helger.base.state.EChange;
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

public class MongoUserGroupManager extends AbstractMongoManager <IUserGroup, UserGroup> implements IUserGroupManager
{
  public static final String GROUP_COLLECTION_NAME = "user-groups";

  private static final String BSON_USER_GROUP_NAME = "name";
  private static final String BSON_USER_GROUP_DESCRIPTION = "description";
  private static final String BSON_USER_GROUP_ROLES = "userIDs";
  private static final String BSON_USER_GROUP_USERS = "roleIDs";

  private final IUserManager m_aUserMgr;
  private final IRoleManager m_aRoleMgr;

  private final CallbackList <IUserGroupModificationCallback> m_aCallbacks = new CallbackList <> ();

  public MongoUserGroupManager (@NonNull final IUserManager aUserMgr, @NonNull final IRoleManager aRoleMgr)
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

  @NonNull
  private UserGroup _internalCreateNewUserGroup (@NonNull final UserGroup aUserGroup, final boolean bPredefined)
  {
    if (!getCollection ().insertOne (toBson (aUserGroup)).wasAcknowledged ())
      throw new IllegalStateException ("Failed to insert into MongoDB Collection");

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

  @Override
  public @NonNull EChange deleteUserGroup (@Nullable final String sUserGroupID)
  {
    return deleteEntity (sUserGroupID, () -> m_aCallbacks.forEach (aCB -> aCB.onUserGroupDeleted (sUserGroupID)));
  }

  @Override
  public @NonNull EChange undeleteUserGroup (@Nullable final String sUserGroupID)
  {
    return undeleteEntity (sUserGroupID, () -> m_aCallbacks.forEach (aCB -> aCB.onUserGroupUndeleted (sUserGroupID)));
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
    return genericUpdate (sUserGroupID,
                          Updates.set (BSON_USER_GROUP_NAME, sNewName),
                          true,
                          () -> m_aCallbacks.forEach (aCB -> aCB.onUserGroupRenamed (sUserGroupID)));
  }

  @Override
  public @NonNull EChange setUserGroupData (@Nullable final String sUserGroupID,
                                            @NonNull @Nonempty final String sNewName,
                                            @Nullable final String sNewDescription,
                                            @Nullable final Map <String, String> aNewCustomAttrs)
  {
    return genericUpdate (sUserGroupID,
                          Updates.combine (Updates.set (BSON_USER_GROUP_NAME, sNewName),
                                           Updates.set (BSON_USER_GROUP_DESCRIPTION, sNewDescription),
                                           Updates.set (BSON_ATTRIBUTES, aNewCustomAttrs)),
                          true,
                          () -> m_aCallbacks.forEach (aCB -> aCB.onUserGroupUpdated (sUserGroupID)));
  }

  @Override
  public @NonNull EChange assignUserToUserGroup (@Nullable final String sUserGroupID,
                                                 @NonNull @Nonempty final String sUserID)
  {
    return genericUpdate (sUserGroupID,
                          Updates.push (BSON_USER_GROUP_USERS, sUserID),
                          true,
                          () -> m_aCallbacks.forEach (aCB -> aCB.onUserGroupUserAssignment (sUserGroupID,
                                                                                            sUserID,
                                                                                            true)));
  }

  @Override
  public @NonNull EChange unassignUserFromUserGroup (@Nullable final String sUserGroupID,
                                                     @Nullable final String sUserID)
  {
    return genericUpdate (sUserGroupID,
                          Updates.pull (BSON_USER_GROUP_USERS, sUserID),
                          true,
                          () -> m_aCallbacks.forEach (aCB -> aCB.onUserGroupUserAssignment (sUserGroupID,
                                                                                            sUserID,
                                                                                            false)));
  }

  @Override
  public @NonNull EChange unassignUserFromAllUserGroups (@Nullable final String sUserID)
  {
    final UpdateResult updateResult = getCollection ().updateMany (new Document (),
                                                                   addLastModToUpdate (Updates.pull (BSON_USER_GROUP_USERS,
                                                                                                     sUserID)));
    if (updateResult.getMatchedCount () > 0)
    {
      m_aCallbacks.forEach (aCB -> aCB.onUserGroupUserAssignment (null, sUserID, false)); // not
                                                                                          // supported
                                                                                          // in
                                                                                          // mongodb
      return EChange.CHANGED;
    }
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
    return getAllUserGroupsWithAssignedUser (sUserID).getAllMapped (IHasID::getID);
  }

  @Override
  public @NonNull EChange assignRoleToUserGroup (@Nullable final String sUserGroupID,
                                                 @NonNull @Nonempty final String sRoleID)
  {
    return genericUpdate (sUserGroupID,
                          Updates.push (BSON_USER_GROUP_ROLES, sRoleID),
                          true,
                          () -> m_aCallbacks.forEach (aCB -> aCB.onUserGroupRoleAssignment (sUserGroupID,
                                                                                            sRoleID,
                                                                                            true)));
  }

  @Override
  public @NonNull EChange unassignRoleFromUserGroup (@Nullable final String sUserGroupID,
                                                     @Nullable final String sRoleID)
  {
    return genericUpdate (sUserGroupID,
                          Updates.pull (BSON_USER_GROUP_ROLES, sRoleID),
                          true,
                          () -> m_aCallbacks.forEach (aCB -> aCB.onUserGroupRoleAssignment (sUserGroupID,
                                                                                            sRoleID,
                                                                                            false)));
  }

  @Override
  public @NonNull EChange unassignRoleFromAllUserGroups (@Nullable final String sRoleID)
  {
    return getCollection ().updateMany (new Document (),
                                        addLastModToUpdate (Updates.pull (BSON_USER_GROUP_ROLES, sRoleID)))
                           .getMatchedCount () > 0 ? EChange.CHANGED : EChange.UNCHANGED;
  }

  @Override
  public @NonNull ICommonsList <IUserGroup> getAllUserGroupsWithAssignedRole (@Nullable final String sRoleID)
  {
    return findAll (Filters.eq (BSON_USER_GROUP_ROLES, sRoleID));
  }

  @Override
  public @NonNull ICommonsList <String> getAllUserGroupIDsWithAssignedRole (@Nullable final String sRoleID)
  {
    return getAllUserGroupsWithAssignedRole (sRoleID).getAllMapped (IHasID::getID);
  }

  @Override
  public boolean containsUserGroupWithAssignedRole (@Nullable final String sRoleID)
  {
    return getCollection ().countDocuments (Filters.eq (BSON_USER_GROUP_ROLES, sRoleID)) > 0;
  }

  @Override
  public boolean containsAnyUserGroupWithAssignedUserAndRole (@Nullable final String sUserID,
                                                              @Nullable final String sRoleID)
  {
    return getCollection ().countDocuments (Filters.and (Filters.eq (BSON_USER_GROUP_USERS, sUserID),
                                                         Filters.eq (BSON_USER_GROUP_ROLES, sRoleID))) > 0;
  }

}
