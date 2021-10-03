/**
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

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
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
import com.helger.photon.security.role.IRole;
import com.helger.photon.security.role.IRoleManager;
import com.helger.photon.security.role.IRoleModificationCallback;
import com.helger.photon.security.role.Role;
import com.helger.photon.security.role.RoleManager;

/**
 * Implementation of {@link IRoleManager} for JDBC backends.
 *
 * @author Philip Helger
 */
public class RoleManagerJDBC extends AbstractJDBCEnabledSecurityManager implements IRoleManager
{
  private final CallbackList <IRoleModificationCallback> m_aCallbacks = new CallbackList <> ();

  public RoleManagerJDBC (@Nonnull final Supplier <? extends DBExecutor> aDBExecSupplier)
  {
    super (aDBExecSupplier);
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IRole> getAll ()
  {
    final ICommonsList <IRole> ret = new CommonsArrayList <> ();
    final ICommonsList <DBResultRow> aDBResult = newExecutor ().queryAll ("SELECT id, creationdt, creationuserid, lastmoddt, lastmoduserid, deletedt, deleteuserid, attrs," +
                                                                          " name, description" +
                                                                          " FROM smp_secrole");
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
        ret.add (new Role (aStub, aRow.getAsString (8), aRow.getAsString (9)));
      }
    return ret;
  }

  public boolean containsWithID (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return false;

    return newExecutor ().queryCount ("SELECT COUNT(*) FROM smp_secrole WHERE id=?", new ConstantPreparedStatementDataProvider (sID)) > 0;
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
    if (!containsWithID (CSecurity.ROLE_ADMINISTRATOR_ID))
      _internalCreateItem (RoleManager.createDefaultRoleAdministrator ());
    if (!containsWithID (CSecurity.ROLE_USER_ID))
      _internalCreateItem (RoleManager.createDefaultRoleUser ());
  }

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <IRoleModificationCallback> roleModificationCallbacks ()
  {
    return m_aCallbacks;
  }

  @Nonnull
  private ESuccess _internalCreateItem (@Nonnull final Role aRole)
  {
    final DBExecutor aExecutor = newExecutor ();
    return aExecutor.performInTransaction ( () -> {
      // Create new
      final long nCreated = aExecutor.insertOrUpdateOrDelete ("INSERT INTO smp_secrole (id, creationdt, creationuserid, lastmoddt, lastmoduserid, deletedt, deleteuserid, attrs," +
                                                              " name, description)" +
                                                              " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                                              new ConstantPreparedStatementDataProvider (getTrimmedToLength (aRole.getID (),
                                                                                                                             45),
                                                                                                         toTimestamp (aRole.getCreationDateTime ()),
                                                                                                         getTrimmedToLength (aRole.getCreationUserID (),
                                                                                                                             20),
                                                                                                         toTimestamp (aRole.getLastModificationDateTime ()),
                                                                                                         getTrimmedToLength (aRole.getLastModificationUserID (),
                                                                                                                             20),
                                                                                                         toTimestamp (aRole.getDeletionDateTime ()),
                                                                                                         getTrimmedToLength (aRole.getDeletionUserID (),
                                                                                                                             20),
                                                                                                         attrsToString (aRole.attrs ()),
                                                                                                         getTrimmedToLength (aRole.getName (),
                                                                                                                             255),
                                                                                                         aRole.getDescription ()));
      if (nCreated != 1)
        throw new IllegalStateException ("Failed to create new DB entry (" + nCreated + ")");
    });
  }

  @Nonnull
  private void _createNewRole (@Nonnull final Role aRole, final boolean bPredefined)
  {
    // Store
    if (_internalCreateItem (aRole).isFailure ())
    {
      AuditHelper.onAuditCreateFailure (Role.OT,
                                        aRole.getID (),
                                        aRole.getName (),
                                        aRole.getDescription (),
                                        bPredefined ? "predefined" : "custom",
                                        "database-error");
    }
    else
    {
      AuditHelper.onAuditCreateSuccess (Role.OT,
                                        aRole.getID (),
                                        aRole.getName (),
                                        aRole.getDescription (),
                                        bPredefined ? "predefined" : "custom");

      // Execute callback as the very last action
      m_aCallbacks.forEach (aCB -> aCB.onRoleCreated (aRole, bPredefined));
    }
  }

  @Nonnull
  public IRole createNewRole (@Nonnull @Nonempty final String sName,
                              @Nullable final String sDescription,
                              @Nullable final Map <String, String> aCustomAttrs)
  {
    // Create role
    final Role aRole = new Role (sName, sDescription, aCustomAttrs);
    _createNewRole (aRole, false);
    return aRole;
  }

  @Nonnull
  public IRole createPredefinedRole (@Nonnull @Nonempty final String sID,
                                     @Nonnull @Nonempty final String sName,
                                     @Nullable final String sDescription,
                                     @Nullable final Map <String, String> aCustomAttrs)
  {
    // Create role
    final Role aRole = new Role (StubObject.createForCurrentUserAndID (sID, aCustomAttrs), sName, sDescription);
    _createNewRole (aRole, true);
    return aRole;
  }

  @Nonnull
  public EChange deleteRole (@Nullable final String sRoleID)
  {
    if (StringHelper.hasNoText (sRoleID))
      return EChange.UNCHANGED;

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secrole SET deletedt=?, deleteuserid=? WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                         getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                             20),
                                                                                                         sRoleID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditDeleteFailure (Role.OT, sRoleID, "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such user ID
      AuditHelper.onAuditDeleteFailure (Role.OT, sRoleID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditDeleteSuccess (Role.OT, sRoleID);

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onRoleDeleted (sRoleID));

    return EChange.CHANGED;
  }

  @Nullable
  public IRole getRoleOfID (@Nullable final String sRoleID)
  {
    if (StringHelper.hasNoText (sRoleID))
      return null;

    final Wrapper <DBResultRow> aDBResult = new Wrapper <> ();
    newExecutor ().querySingle ("SELECT creationdt, creationuserid, lastmoddt, lastmoduserid, deletedt, deleteuserid, attrs," +
                                " name, description" +
                                " FROM smp_secrole" +
                                " WHERE id=?",
                                new ConstantPreparedStatementDataProvider (sRoleID),
                                aDBResult::set);
    if (aDBResult.isNotSet ())
      return null;

    final DBResultRow aRow = aDBResult.get ();
    final StubObject aStub = new StubObject (sRoleID,
                                             aRow.getAsLocalDateTime (0),
                                             aRow.getAsString (1),
                                             aRow.getAsLocalDateTime (2),
                                             aRow.getAsString (3),
                                             aRow.getAsLocalDateTime (4),
                                             aRow.getAsString (5),
                                             attrsToMap (aRow.getAsString (6)));
    return new Role (aStub, aRow.getAsString (7), aRow.getAsString (8));
  }

  @Nonnull
  public EChange renameRole (@Nullable final String sRoleID, @Nonnull @Nonempty final String sNewName)
  {
    if (StringHelper.hasNoText (sRoleID))
      return EChange.UNCHANGED;

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secrole SET name=?, lastmoddt=?, lastmoduserid=? WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (sNewName,
                                                                                                         toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                         getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                             20),
                                                                                                         sRoleID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (Role.OT, "set-name", sRoleID, sNewName, "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such role ID
      AuditHelper.onAuditModifyFailure (Role.OT, "set-name", sRoleID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (Role.OT, "set-name", sRoleID, sNewName);

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onRoleRenamed (sRoleID));

    return EChange.CHANGED;
  }

  @Nonnull
  public EChange setRoleData (@Nullable final String sRoleID,
                              @Nonnull @Nonempty final String sNewName,
                              @Nullable final String sNewDescription,
                              @Nullable final Map <String, String> aNewCustomAttrs)
  {
    if (StringHelper.hasNoText (sRoleID))
      return EChange.UNCHANGED;

    final MutableLong aUpdated = new MutableLong (-1);
    final DBExecutor aExecutor = newExecutor ();
    final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
      // Update existing
      final long nUpdated = aExecutor.insertOrUpdateOrDelete ("UPDATE smp_secrole SET name=?, description=?, attrs=?, lastmoddt=?, lastmoduserid=? WHERE id=?",
                                                              new ConstantPreparedStatementDataProvider (sNewName,
                                                                                                         sNewDescription,
                                                                                                         attrsToString (aNewCustomAttrs),
                                                                                                         toTimestamp (PDTFactory.getCurrentLocalDateTime ()),
                                                                                                         getTrimmedToLength (BusinessObjectHelper.getUserIDOrFallback (),
                                                                                                                             20),
                                                                                                         sRoleID));
      aUpdated.set (nUpdated);
    });

    if (eSuccess.isFailure ())
    {
      // DB error
      AuditHelper.onAuditModifyFailure (Role.OT, "set-all", sRoleID, sNewName, sNewDescription, aNewCustomAttrs, "database-error");
      return EChange.UNCHANGED;
    }

    if (aUpdated.is0 ())
    {
      // No such role ID
      AuditHelper.onAuditModifyFailure (Role.OT, "set-all", sRoleID, "no-such-id");
      return EChange.UNCHANGED;
    }

    AuditHelper.onAuditModifySuccess (Role.OT, "set-all", sRoleID, sNewName, sNewDescription, aNewCustomAttrs);

    // Execute callback as the very last action
    m_aCallbacks.forEach (aCB -> aCB.onRoleUpdated (sRoleID));

    return EChange.CHANGED;
  }
}
