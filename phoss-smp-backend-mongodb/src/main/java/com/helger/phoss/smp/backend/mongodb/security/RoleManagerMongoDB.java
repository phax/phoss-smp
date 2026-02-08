/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.mongodb.security;

import java.util.Map;

import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.misc.DevelopersNote;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.security.object.StubObject;
import com.helger.photon.security.role.IRole;
import com.helger.photon.security.role.IRoleManager;
import com.helger.photon.security.role.IRoleModificationCallback;
import com.helger.photon.security.role.Role;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;

public class RoleManagerMongoDB extends AbstractBusinessObjectManagerMongoDB <IRole, Role> implements IRoleManager
{
  public static final String ROLE_COLLECTION_NAME = "user-roles";

  private static final String BSON_ROLE_NAME = "name";
  private static final String BSON_ROLE_DESCRIPTION = "description";

  private final CallbackList <IRoleModificationCallback> m_aCallbacks = new CallbackList <> ();

  public RoleManagerMongoDB ()
  {
    super (ROLE_COLLECTION_NAME);
    getCollection ().createIndex (Indexes.ascending (BSON_ID));
  }

  @NonNull
  @Override
  @ReturnsMutableCopy
  protected Document toBson (@NonNull final IRole aRole)
  {
    return getDefaultBusinessDocument (aRole).append (BSON_ROLE_NAME, aRole.getName ())
                                             .append (BSON_ROLE_DESCRIPTION, aRole.getDescription ());
  }

  @NonNull
  @ReturnsMutableCopy
  @Override
  protected Role toEntity (@NonNull final Document aDoc)
  {
    return new Role (populateStubObject (aDoc),
                     aDoc.getString (BSON_ROLE_NAME),
                     aDoc.getString (BSON_ROLE_DESCRIPTION));
  }

  @Override
  public void createDefaultsForTest ()
  {
    // ignored for now
  }

  @Override
  @ReturnsMutableObject
  public @NonNull CallbackList <IRoleModificationCallback> roleModificationCallbacks ()
  {
    return m_aCallbacks;
  }

  @NonNull
  private Role _internalCreateNewRole (@NonNull final Role aRole, final boolean bPredefined)
  {
    if (!getCollection ().insertOne (toBson (aRole)).wasAcknowledged ())
    {
      AuditHelper.onAuditCreateFailure (Role.OT,
                                        aRole.getID (),
                                        aRole.getName (),
                                        aRole.getDescription (),
                                        bPredefined ? "predefined" : "custom",
                                        "database-error");
      return null;
    }

    AuditHelper.onAuditCreateSuccess (Role.OT,
                                      aRole.getID (),
                                      aRole.getName (),
                                      aRole.getDescription (),
                                      bPredefined ? "predefined" : "custom");

    m_aCallbacks.forEach (aCB -> aCB.onRoleCreated (aRole, bPredefined));
    return aRole;
  }

  @Override
  public @Nullable IRole createNewRole (@NonNull @Nonempty final String sName,
                                        @Nullable final String sDescription,
                                        @Nullable final Map <String, String> aCustomAttrs)
  {
    // Create role
    final Role aRole = new Role (sName, sDescription, aCustomAttrs);
    return _internalCreateNewRole (aRole, false);
  }

  @Override
  public @Nullable IRole createPredefinedRole (@NonNull @Nonempty final String sID,
                                               @NonNull @Nonempty final String sName,
                                               @Nullable final String sDescription,
                                               @Nullable final Map <String, String> aCustomAttrs)
  {
    // Create role
    final Role aRole = new Role (StubObject.createForCurrentUserAndID (sID, aCustomAttrs), sName, sDescription);
    return _internalCreateNewRole (aRole, true);
  }

  @DevelopersNote ("For internal use only")
  public @Nullable IRole internalCreateMigrationRole (@NonNull final Role aSrcRole)
  {
    // Create role
    return _internalCreateNewRole (aSrcRole, true);
  }

  @Override
  public @NonNull EChange deleteRole (@Nullable final String sRoleID)
  {
    if (StringHelper.isEmpty (sRoleID))
      return EChange.UNCHANGED;

    final EChange eChange = deleteEntity (sRoleID);
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditDeleteSuccess (Role.OT, sRoleID);

      m_aCallbacks.forEach (aCB -> aCB.onRoleDeleted (sRoleID));
    }
    else
    {
      AuditHelper.onAuditDeleteFailure (Role.OT, sRoleID, "no-such-id");
    }
    return eChange;
  }

  @VisibleForTesting
  void internalDeleteRoleNotRecoverable (@NonNull final String sRoleID)
  {
    getCollection ().deleteOne (Filters.eq (BSON_ID, sRoleID));
  }

  @Override
  public @Nullable IRole getRoleOfID (@Nullable final String sRoleID)
  {
    return findByID (sRoleID);
  }

  @Override
  public @NonNull EChange renameRole (@Nullable final String sRoleID, @NonNull @Nonempty final String sNewName)
  {
    if (StringHelper.isEmpty (sRoleID))
      return EChange.UNCHANGED;

    final EChange eChange = genericUpdateOne (sRoleID, addLastModToUpdate (Updates.set (BSON_ROLE_NAME, sNewName)));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (Role.OT, "set-name", sRoleID, sNewName);

      m_aCallbacks.forEach (aCB -> aCB.onRoleRenamed (sRoleID));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (Role.OT, "set-name", sRoleID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange setRoleData (@Nullable final String sRoleID,
                                       @NonNull @Nonempty final String sNewName,
                                       @Nullable final String sNewDescription,
                                       @Nullable final Map <String, String> aNewCustomAttrs)
  {
    if (StringHelper.isEmpty (sRoleID))
      return EChange.UNCHANGED;

    final EChange eChange = genericUpdateOne (sRoleID,
                                              addLastModToUpdate (Updates.combine (Updates.set (BSON_ROLE_NAME,
                                                                                                sNewName),
                                                                                   Updates.set (BSON_ROLE_DESCRIPTION,
                                                                                                sNewDescription),
                                                                                   Updates.set (BSON_ATTRIBUTES,
                                                                                                aNewCustomAttrs))));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (Role.OT, "set-all", sRoleID, sNewName, sNewDescription, aNewCustomAttrs);

      m_aCallbacks.forEach (aCB -> aCB.onRoleUpdated (sRoleID));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (Role.OT, "set-all", sRoleID, "no-such-id");
    }
    return eChange;
  }
}
