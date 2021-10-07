/*
 * Copyright (C) 2019-2021 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.sql.security;

import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.mutable.MutableBoolean;
import com.helger.commons.mutable.MutableLong;
import com.helger.commons.state.EChange;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.wrapper.Wrapper;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.object.BusinessObjectHelper;
import com.helger.photon.security.object.StubObject;
import com.helger.photon.security.role.IRoleManager;
import com.helger.photon.security.user.IUserManager;
import com.helger.photon.security.usergroup.IUserGroup;
import com.helger.photon.security.usergroup.IUserGroupManager;
import com.helger.photon.security.usergroup.IUserGroupModificationCallback;
import com.helger.photon.security.usergroup.UserGroup;
import com.helger.photon.security.usergroup.UserGroupManager;

/**
 * Implementation of {@link IUserGroupManager} for JDBC backends.
 *
 * @author Philip Helger
 */
public class UserGroupManagerJDBC extends AbstractJDBCEnabledSecurityManager implements IUserGroupManager
{
  private final IUserManager m_aUserMgr;
  private final IRoleManager m_aRoleMgr;

  private final CallbackList <IUserGroupModificationCallback> m_aCallbacks = new CallbackList <> ();

  public UserGroupManagerJDBC (@Nonnull final Supplier <? extends DBExecutor> aDBExecSupplier,
                               @Nonnull final IUserManager aUserMgr,
                               @Nonnull final IRoleManager aRoleMgr)
  {
    super (aDBExecSupplier);
    m_aUserMgr = ValueEnforcer.notNull (aUserMgr, "UserManager");
    m_aRoleMgr = ValueEnforcer.notNull (aRoleMgr, "RoleManager");
  }

  @Nonnull
  public final IUserManager getUserManager ()
  {
    return m_aUserMgr;
  }

  @Nonnull
  public final IRoleManager getRoleManager ()
  {
    return m_aRoleMgr;
  }

  @Nonnull
  @ReturnsMutableCopy
  private ICommonsList <IUserGroup> _getAllWhere (@Nullable final String sCondition,
                                                  @Nullable final ConstantPreparedStatementDataProvider aDataProvider)
  {
    final ICommonsList <IUserGroup> ret = new CommonsArrayList <> ();
    final ICommonsList <DBResultRow> aDBResult;
    String sSQL = "SELECT id, creationdt, creationuserid, lastmoddt, lastmoduserid, deletedt, deleteuserid, attrs," +
                  " name, description, userids, roleids" +
                  " FROM smp_secusergroup";
    if (StringHelper.hasText (sCondition))
    {
      // Condition present
      sSQL += " WHERE " + sCondition;
      if (aDataProvider != null)
        aDBResult = newExecutor ().queryAll (sSQL, aDataProvider);
      else
        aDBResult = newExecutor ().queryAll (sSQL);
    }
    else
    {
      // Simply all
      aDBResult = newExecutor ().queryAll (sSQL);
    }

    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
      {
        final StubObject aStub = new StubObject (aRow.getAsString (0),
                                                 aRow.getAsLocalDateTime (1),
                                                 aRow.getAsString (2),
                                                 aRow.getAsLocalDateTime (3),
                                                 aRow.getAsString (4),
                                                 aRow.getAsLocalDateTime (5),
                                                 aRow.getAsString (6),
                                                 attrsToMap (aRow.getAsString (7)));
        final UserGroup aUserGroup = new UserGroup (aStub, aRow.getAsString (8), aRow.getAsString (9));
        aUserGroup.assignUsers (idsToSet (aRow.getAsString (10)));
        aUserGroup.assignRoles (idsToSet (aRow.getAsString (11)));
        ret.add (aUserGroup);
      }
    return ret;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IUserGroup> getAll ()
  {
    return _getAllWhere (null, null);
  }

  public boolean containsWithID (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return false;

    return newExecutor ().queryCount ("SELECT COUNT(*) FROM smp_secusergroup WHERE id=?",
                                      new ConstantPreparedStatementDataProvider (sID)) > 0;
  }

  public boolean containsAllIDs (@Nullable final Iterable <String> aIDs)
  {
    if (aIDs != null)
    {
      // TODO could be optimized
      for (final String sID : aIDs)
        if (!containsWithID (sID))
          return false;
    }
    return true;
  }

  public void createDefaultsForTest ()
  {
    // Administrators user group
    UserGroup aUG = getUserGroupOfID (CSecurity.USERGROUP_ADMINISTRATORS_ID);
    if (aUG == null)
    {
      aUG = UserGroupManager.createDefaultUserGroupAdministrators ();
      _internalCreateItem (aUG);
    }
    if (m_aUserMgr.containsWithID (CSecurity.USER_ADMINISTRATOR_ID))
      aUG.assignUser (CSecurity.USER_ADMINISTRATOR_ID);
    if (m_aRoleMgr.containsWithID (CSecurity.ROLE_ADMINISTRATOR_ID))
      aUG.assignRole (CSecurity.ROLE_ADMINISTRATOR_ID);

    // Users user group
    aUG = getUserGroupOfID (CSecurity.USERGROUP_USERS_ID);
    if (aUG == null)
    {
      aUG = UserGroupManager.createDefaultUserGroupUsers ();
      _internalCreateItem (aUG);
    }
    if (m_aUserMgr.containsWithID (CSecurity.USER_USER_ID))
      aUG.assignUser (CSecurity.USER_USER_ID);
    if (m_aRoleMgr.containsWithID (CSecurity.ROLE_USER_ID))
      aUG.assignRole (CSecurity.ROLE_USER_ID);

    // Guests user group
    aUG = getUserGroupOfID (CSecurity.USERGROUP_GUESTS_ID);
    if (aUG == null)
    {
      aUG = UserGroupManager.createDefaultUserGroupGuests ();
      _internalCreateItem (aUG);
    }
    if (m_aUserMgr.containsWithID (CSecurity.USER_GUEST_ID))
      aUG.assignUser (CSecurity.USER_GUEST_ID);
    // no role for this user group
  }

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <IUserGroupModificationCallback> userGroupModificationCallbacks ()
  {
    return m_aCallbacks;
  }

  @Nonnull
  private ESuccess _internalCreateItem (@Nonnull final UserGroup aUserGroup)
  {
    final DBExecutor aExecutor = newExecutor ();
    return aExecutor.performInTransaction ( () -> {
      // Create new
      final long nCreated = aExecutor.insertOrUpdateOrDelete ("INSERT INTO smp_secusergroup (id, creationdt, creationuserid, lastmoddt, lastmoduserid, deletedt, deleteuserid, attrs," +
                                                              " name, description, userids, roleids)" +
                                                              " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                                              new ConstantPreparedStatementDataProvider (getTrimmedToLength (aUserGroup.getID (),
                                                                                                                             45),
                                                                                                         toTimestamp (aUserGroup.getCreationDateTime ()),
                                                                                                         getTrimmedToLength (aUserGroup.getCreationUserID (),
                                                                                                                             20),
                                                                                                         toTimestamp (aUserGroup.getLastModificationDateTime ()),
                                                                                                         getTrimmedToLength (aUserGroup.getLastModificationUserID (),
                                                                                                                             20),
                                                                                                         toTimestamp (aUserGroup.getDeletionDateTime ()),
                                                                                                         getTrimmedToLength (aUserGroup.getDeletionUserID (),
                                                                                                                             20),
                                                                                                         attrsToString (aUserGroup.attrs ()),
                                                                                                         getTrimmedToLength (aUserGroup.getName (),
                                                                                                                             255),
                                                                                                         aUserGroup.getDescription (),
                                                                                                         idsToString (aUserGroup.getAllContainedUserIDs ()),
                                                                                                         idsToString (aUserGroup.getAllContainedRoleIDs ())));
      if (nCreated != 1)
        throw new IllegalStateException ("Failed to create new DB entry (" + nCreated + ")");
    });
  }

  @Nullable
  public UserGroup internalCreateNewUserGroup (@Nonnull final UserGroup aUserGroup, final boolean bPredefined, final boolean bRunCallback)
  {
    // Store
    if (_internalCreateItem (aUserGroup).isFailure ())
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

    if (bRunCallback)
    {
      // Execute callback as the very last action
      m_aCallbacks.forEach (aCB -> aCB.onUserGroupCreated (aUserGroup, bPredefined));
    }

    return aUserGroup;
  }

  @Nullable
  public IUserGroup createNewUserGroup (@Nonnull @Nonempty final String sName,
                                        @Nullable final String sDescription,
                                        @Nullable final Map <String, String> aCustomAttrs)
  {
    // Create user group
    final UserGroup aUserGroup = new UserGroup (sName, sDescription, aCustomAttrs);
    return internalCreateNewUserGroup (aUserGroup, false, true);
  }

  @Nullable
  public IUserGroup createPredefinedUserGroup (@Nonnull @Nonempty final String sID,
                                               @Nonnull @Nonempty final String sName,
                                               @Nullable final String sDescription,
                                               @Nullable final Map <String, String> aCustomAttrs)
  {
    // Create user group
    final UserGroup aUserGroup = new UserGroup (StubObject.createForCurrentUserAndID (sID, aCustomAttrs), sName, sDescription);
    return internalCreateNewUserGroup (aUserGroup, true, true);
  }

  @Nonnull
  public EChange deleteUserGroup (@Nullable final String sUserGroupID)
  {
    if (StringHelper.hasNoText (sUserGroupID))
      return EChange.UNCHANGED;

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secusergroup SET deletedt=?, deleteuserid=? WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                         getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                             20),
                                                                                                         sUserGroupID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditDeleteFailure (UserGroup.OT, sUserGroupID, "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user ID
      AuditHelper.onAuditDeleteFailure (UserGroup.OT, sUserGroupID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditDeleteSuccess (UserGroup.OT, sUserGroupID);

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onUserGroupDeleted (sUserGroupID));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange undeleteUserGroup (@Nullable final String sUserGroupID)
  {
    if (StringHelper.hasNoText (sUserGroupID))
      return EChange.UNCHANGED;

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secusergroup SET lastmoddt=?, lastmoduserid=?, deletedt=NULL, deleteuserid=NULL WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                         getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                             20),
                                                                                                         sUserGroupID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditUndeleteFailure (UserGroup.OT, sUserGroupID, "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user ID
      AuditHelper.onAuditUndeleteFailure (UserGroup.OT, sUserGroupID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditUndeleteSuccess (UserGroup.OT, sUserGroupID);

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onUserGroupUndeleted (sUserGroupID));

    return EChange.CHANGED;
  }

  @Nullable
  public UserGroup getUserGroupOfID (@Nullable final String sUserGroupID)
  {
    if (StringHelper.hasNoText (sUserGroupID))
      return null;

    final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
    newExecutor ().querySingle ("SELECT creationdt, creationuserid, lastmoddt, lastmoduserid, deletedt, deleteuserid, attrs," +
                                " name, description, userids, roleids" +
                                " FROM smp_secusergroup" +
                                " WHERE id=?",
                                new ConstantPreparedStatementDataProvider (sUserGroupID),
                                aDBResult::set);
    if (aDBResult.isNotSet ())
      return null;

    final DBResultRow aRow = aDBResult.get ();
    final StubObject aStub = new StubObject (sUserGroupID,
                                             aRow.getAsLocalDateTime (0),
                                             aRow.getAsString (1),
                                             aRow.getAsLocalDateTime (2),
                                             aRow.getAsString (3),
                                             aRow.getAsLocalDateTime (4),
                                             aRow.getAsString (5),
                                             attrsToMap (aRow.getAsString (6)));
    final UserGroup aUserGroup = new UserGroup (aStub, aRow.getAsString (7), aRow.getAsString (8));
    aUserGroup.assignUsers (idsToSet (aRow.getAsString (9)));
    aUserGroup.assignRoles (idsToSet (aRow.getAsString (10)));
    return aUserGroup;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IUserGroup> getAllActiveUserGroups ()
  {
    return _getAllWhere ("deletedt IS NULL", null);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IUserGroup> getAllDeletedUserGroups ()
  {
    return _getAllWhere ("deletedt IS NOT NULL", null);
  }

  @Nonnull
  public EChange renameUserGroup (@Nullable final String sUserGroupID, @Nonnull @Nonempty final String sNewName)
  {
    if (StringHelper.hasNoText (sUserGroupID))
      return EChange.UNCHANGED;

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secusergroup SET name=?, lastmoddt=?, lastmoduserid=? WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (sNewName,
                                                                                                         toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                         getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                             20),
                                                                                                         sUserGroupID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "set-name", sUserGroupID, sNewName, "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user group ID
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "set-name", sUserGroupID, "no-such-id");
      return EChange.UNCHANGED;
    }
    AuditHelper.onAuditModifySuccess (UserGroup.OT, "set-name", sUserGroupID, sNewName);

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onUserGroupRenamed (sUserGroupID));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange setUserGroupData (@Nullable final String sUserGroupID,
                                   @Nonnull @Nonempty final String sNewName,
                                   @Nullable final String sNewDescription,
                                   @Nullable final Map <String, String> aNewCustomAttrs)
  {
    if (StringHelper.hasNoText (sUserGroupID))
      return EChange.UNCHANGED;

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secusergroup SET name=?, description=?, attrs=?, lastmoddt=?, lastmoduserid=? WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (sNewName,
                                                                                                         sNewDescription,
                                                                                                         attrsToString (aNewCustomAttrs),
                                                                                                         toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                         getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                             20),
                                                                                                         sUserGroupID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (UserGroup.OT,
                                        "set-all",
                                        sUserGroupID,
                                        sNewName,
                                        sNewDescription,
                                        aNewCustomAttrs,
                                        "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user group ID
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "set-all", sUserGroupID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (UserGroup.OT, "set-all", sUserGroupID, sNewName, sNewDescription, aNewCustomAttrs);

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onUserGroupUpdated (sUserGroupID));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange assignUserToUserGroup (@Nullable final String sUserGroupID, @Nonnull @Nonempty final String sUserID)
  {
    ValueEnforcer.notEmpty (sUserID, "UserID");
    if (StringHelper.hasNoText (sUserGroupID))
      return EChange.UNCHANGED;

    final MutableBoolean aWasAdded = new MutableBoolean (false);
    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Get existing users
      final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
      newExecutor ().querySingle ("SELECT userids FROM smp_secusergroup WHERE id=?",
                                  new ConstantPreparedStatementDataProvider (sUserGroupID),
                                  aDBResult::set);
      ICommonsSet <String> aAssignedIDs = aDBResult.isNotSet () ? null : idsToSet (aDBResult.get ().getAsString (0));
      if (aAssignedIDs == null)
        aAssignedIDs = new CommonsHashSet <> ();

      if (aAssignedIDs.add (sUserID))
      {
        aWasAdded.set (true);

        // Update existing
        final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secusergroup SET userids=?, lastmoddt=?, lastmoduserid=? WHERE id=?",
                                                                new ConstantPreparedStatementDataProvider (idsToString (aAssignedIDs),
                                                                                                           toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                           getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                               20),
                                                                                                           sUserGroupID));
        aUpdated.set (nUpdated);
      }
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "assign-user", sUserGroupID, sUserID, "database-error");
      return EChange.UNCHANGED;
    }

    if (!aWasAdded.booleanValue ())
    {
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "assign-user", sUserGroupID, sUserID, "already-assigned");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user group ID
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "assign-user", sUserGroupID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (UserGroup.OT, "assign-user", sUserGroupID, sUserID);

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onUserGroupUserAssignment (sUserGroupID, sUserID, true));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange unassignUserFromUserGroup (@Nullable final String sUserGroupID, @Nullable final String sUserID)
  {
    if (StringHelper.hasNoText (sUserGroupID))
      return EChange.UNCHANGED;
    if (StringHelper.hasNoText (sUserID))
      return EChange.UNCHANGED;

    final MutableBoolean aWasRemoved = new MutableBoolean (false);
    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Get existing users
      final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
      newExecutor ().querySingle ("SELECT userids FROM smp_secusergroup WHERE id=?",
                                  new ConstantPreparedStatementDataProvider (sUserGroupID),
                                  aDBResult::set);
      final ICommonsSet <String> aAssignedIDs = aDBResult.isNotSet () ? null : idsToSet (aDBResult.get ().getAsString (0));

      if (aAssignedIDs != null && aAssignedIDs.remove (sUserID))
      {
        aWasRemoved.set (true);

        // Update existing
        final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secusergroup SET userids=?, lastmoddt=?, lastmoduserid=? WHERE id=?",
                                                                new ConstantPreparedStatementDataProvider (idsToString (aAssignedIDs),
                                                                                                           toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                           getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                               20),
                                                                                                           sUserGroupID));
        aUpdated.set (nUpdated);
      }
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "unassign-user", sUserGroupID, sUserID, "database-error");
      return EChange.UNCHANGED;
    }

    if (!aWasRemoved.booleanValue ())
    {
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "unassign-user", sUserGroupID, sUserID, "not-assigned");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user group ID
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "unassign-user", sUserGroupID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (UserGroup.OT, "unassign-user", sUserGroupID, sUserID);

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onUserGroupUserAssignment (sUserGroupID, sUserID, false));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange unassignUserFromAllUserGroups (@Nullable final String sUserID)
  {
    if (StringHelper.hasNoText (sUserID))
      return EChange.UNCHANGED;

    final ICommonsList <String> aAffectedUserGroups = new CommonsArrayList <> ();
    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Get all existing assignments
      final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT id, userids FROM smp_secusergroup");
      for (final DBResultRow aRow : aRows)
      {
        final String sUserGroupID = aRow.getAsString (0);
        final String sAssignedIDs = aRow.getAsString (1);
        final ICommonsSet <String> aAssignedIDs = idsToSet (sAssignedIDs);
        if (aAssignedIDs != null && aAssignedIDs.remove (sUserID))
        {
          aAffectedUserGroups.add (sUserGroupID);

          // Update existing
          final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secusergroup SET userids=?, lastmoddt=?, lastmoduserid=? WHERE id=?",
                                                                  new ConstantPreparedStatementDataProvider (idsToString (aAssignedIDs),
                                                                                                             toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                             getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                                 20),
                                                                                                             sUserGroupID));
          aUpdated.inc (nUpdated);
        }
      }
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "unassign-user-from-all-usergroups", sUserID, "database-error");
      return EChange.UNCHANGED;
    }

    if (aAffectedUserGroups.isEmpty ())
    {
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "unassign-user-from-all-usergroups", sUserID, "not-assigned");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (UserGroup.OT, "unassign-user-from-all-usergroups", sUserID);

    // Execute callback as the very last action
    for (final String sUserGroupID : aAffectedUserGroups)
      m_aCallbacks.forEach (aCB -> aCB.onUserGroupUserAssignment (sUserGroupID, sUserID, false));

    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IUserGroup> getAllUserGroupsWithAssignedUser (@Nullable final String sUserID)
  {
    if (StringHelper.hasNoText (sUserID))
      return new CommonsArrayList <> ();

    // Limit from the SQL point as much as possible and filter the results here
    return _getAllWhere ("userids LIKE ?",
                         new ConstantPreparedStatementDataProvider ("%" + sUserID + "%"))
                                                                                         .getAll (aUserGroup -> aUserGroup.containsUserID (sUserID));
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getAllUserGroupIDsWithAssignedUser (@Nullable final String sUserID)
  {
    if (StringHelper.hasNoText (sUserID))
      return new CommonsArrayList <> ();

    return getAllUserGroupsWithAssignedUser (sUserID).getAllMapped (IUserGroup::getID);
  }

  @Nonnull
  public EChange assignRoleToUserGroup (@Nullable final String sUserGroupID, @Nonnull @Nonempty final String sRoleID)
  {
    ValueEnforcer.notEmpty (sRoleID, "RoleID");
    if (StringHelper.hasNoText (sUserGroupID))
      return EChange.UNCHANGED;

    final MutableBoolean aWasAdded = new MutableBoolean (false);
    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Get existing users
      final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
      newExecutor ().querySingle ("SELECT roleids FROM smp_secusergroup WHERE id=?",
                                  new ConstantPreparedStatementDataProvider (sUserGroupID),
                                  aDBResult::set);
      ICommonsSet <String> aAssignedIDs = aDBResult.isNotSet () ? null : idsToSet (aDBResult.get ().getAsString (0));
      if (aAssignedIDs == null)
        aAssignedIDs = new CommonsHashSet <> ();

      if (aAssignedIDs.add (sRoleID))
      {
        aWasAdded.set (true);

        // Update existing
        final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secusergroup SET roleids=?, lastmoddt=?, lastmoduserid=? WHERE id=?",
                                                                new ConstantPreparedStatementDataProvider (idsToString (aAssignedIDs),
                                                                                                           toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                           getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                               20),
                                                                                                           sUserGroupID));
        aUpdated.set (nUpdated);
      }
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "assign-role", sUserGroupID, sRoleID, "database-error");
      return EChange.UNCHANGED;
    }

    if (!aWasAdded.booleanValue ())
    {
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "assign-role", sUserGroupID, sRoleID, "already-assigned");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user group ID
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "assign-role", sUserGroupID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (UserGroup.OT, "assign-role", sUserGroupID, sRoleID);

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onUserGroupRoleAssignment (sUserGroupID, sRoleID, true));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange unassignRoleFromUserGroup (@Nullable final String sUserGroupID, @Nullable final String sRoleID)
  {
    if (StringHelper.hasNoText (sUserGroupID))
      return EChange.UNCHANGED;
    if (StringHelper.hasNoText (sRoleID))
      return EChange.UNCHANGED;

    final MutableBoolean aWasRemoved = new MutableBoolean (false);
    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Get existing users
      final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
      newExecutor ().querySingle ("SELECT roleids FROM smp_secusergroup WHERE id=?",
                                  new ConstantPreparedStatementDataProvider (sUserGroupID),
                                  aDBResult::set);
      final ICommonsSet <String> aAssignedIDs = aDBResult.isNotSet () ? null : idsToSet (aDBResult.get ().getAsString (0));

      if (aAssignedIDs != null && aAssignedIDs.remove (sRoleID))
      {
        aWasRemoved.set (true);

        // Update existing
        final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secusergroup SET roleids=?, lastmoddt=?, lastmoduserid=? WHERE id=?",
                                                                new ConstantPreparedStatementDataProvider (idsToString (aAssignedIDs),
                                                                                                           toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                           getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                               20),
                                                                                                           sUserGroupID));
        aUpdated.set (nUpdated);
      }
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "unassign-role", sUserGroupID, sRoleID, "database-error");
      return EChange.UNCHANGED;
    }

    if (!aWasRemoved.booleanValue ())
    {
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "unassign-role", sUserGroupID, sRoleID, "not-assigned");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user group ID
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "unassign-role", sUserGroupID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (UserGroup.OT, "unassign-role", sUserGroupID, sRoleID);

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onUserGroupRoleAssignment (sUserGroupID, sRoleID, false));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange unassignRoleFromAllUserGroups (@Nullable final String sRoleID)
  {
    if (StringHelper.hasNoText (sRoleID))
      return EChange.UNCHANGED;

    final ICommonsList <String> aAffectedUserGroups = new CommonsArrayList <> ();
    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Get all existing assignments
      final ICommonsList <DBResultRow> aRows = newExecutor ().queryAll ("SELECT id, roleids FROM smp_secusergroup");
      for (final DBResultRow aRow : aRows)
      {
        final String sUserGroupID = aRow.getAsString (0);
        final String sAssignedIDs = aRow.getAsString (1);
        final ICommonsSet <String> aAssignedIDs = idsToSet (sAssignedIDs);
        if (aAssignedIDs != null && aAssignedIDs.remove (sRoleID))
        {
          aAffectedUserGroups.add (sUserGroupID);

          // Update existing
          final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secusergroup SET roleids=?, lastmoddt=?, lastmoduserid=? WHERE id=?",
                                                                  new ConstantPreparedStatementDataProvider (idsToString (aAssignedIDs),
                                                                                                             toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                             getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                                 20),
                                                                                                             sUserGroupID));
          aUpdated.inc (nUpdated);
        }
      }
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "unassign-role-from-all-usergroups", sRoleID, "database-error");
      return EChange.UNCHANGED;
    }

    if (aAffectedUserGroups.isEmpty ())
    {
      AuditHelper.onAuditModifyFailure (UserGroup.OT, "unassign-role-from-all-usergroups", sRoleID, "not-assigned");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (UserGroup.OT, "unassign-role-from-all-usergroups", sRoleID);

    // Execute callback as the very last action
    for (final String sUserGroupID : aAffectedUserGroups)
      m_aCallbacks.forEach (aCB -> aCB.onUserGroupRoleAssignment (sUserGroupID, sRoleID, false));

    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IUserGroup> getAllUserGroupsWithAssignedRole (@Nullable final String sRoleID)
  {
    if (StringHelper.hasNoText (sRoleID))
      return getNone ();

    // Limit from the SQL point as much as possible and filter the results here
    return _getAllWhere ("roleids LIKE ?",
                         new ConstantPreparedStatementDataProvider ("%" + sRoleID + "%"))
                                                                                         .getAll (aUserGroup -> aUserGroup.containsRoleID (sRoleID));
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <String> getAllUserGroupIDsWithAssignedRole (@Nullable final String sRoleID)
  {
    if (StringHelper.hasNoText (sRoleID))
      return getNone ();

    return getAllUserGroupsWithAssignedRole (sRoleID).getAllMapped (IUserGroup::getID);
  }

  public boolean containsUserGroupWithAssignedRole (@Nullable final String sRoleID)
  {
    // Limit from the SQL point as much as possible and filter the results here
    return _getAllWhere ("roleids LIKE ?",
                         new ConstantPreparedStatementDataProvider ("%" + sRoleID + "%"))
                                                                                         .containsAny (aUserGroup -> aUserGroup.containsRoleID (sRoleID));
  }

  public boolean containsAnyUserGroupWithAssignedUserAndRole (@Nullable final String sUserID, @Nullable final String sRoleID)
  {
    if (StringHelper.hasNoText (sUserID))
      return false;
    if (StringHelper.hasNoText (sRoleID))
      return false;

    // Limit from the SQL point as much as possible and filter the results here
    return _getAllWhere ("userids LIKE ? AND roleids LIKE ?",
                         new ConstantPreparedStatementDataProvider ("%" + sUserID + "%", "%" + sRoleID + "%"))
                                                                                                              .containsAny (aUserGroup -> aUserGroup.containsUserID (sUserID) &&
                                                                                                                                          aUserGroup.containsRoleID (sRoleID));
  }
}
