package com.helger.phoss.smp.backend.mongodb.security;

import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.security.object.StubObject;
import com.helger.photon.security.role.IRole;
import com.helger.photon.security.role.IRoleManager;
import com.helger.photon.security.role.IRoleModificationCallback;
import com.helger.photon.security.role.Role;
import com.mongodb.client.model.Updates;

public class MongoRoleManager extends AbstractMongoManager <IRole> implements IRoleManager
{
  public static final String ROLE_COLLECTION_NAME = "user-roles";

  private static final String BSON_ROLE_NAME = "name";
  private static final String BSON_ROLE_DESCRIPTION = "description";

  private final CallbackList <IRoleModificationCallback> m_aCallbacks = new CallbackList <> ();

  public MongoRoleManager ()
  {
    super (ROLE_COLLECTION_NAME);
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
  protected IRole toEntity (@NonNull final Document aDoc)
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
      throw new IllegalStateException ("Failed to insert into MongoDB Collection");

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

  @Override
  public @NonNull EChange deleteRole (@Nullable final String sRoleID)
  {
    if (StringHelper.isEmpty (sRoleID))
      return EChange.UNCHANGED;

    return deleteEntity (sRoleID, () -> m_aCallbacks.forEach (aCB -> aCB.onRoleDeleted (sRoleID)));
  }

  @Override
  public @Nullable IRole getRoleOfID (@Nullable final String sRoleID)
  {
    return findByID (sRoleID);
  }

  @Override
  public @NonNull EChange renameRole (@Nullable final String sRoleID, @NonNull @Nonempty final String sNewName)
  {
    return genericUpdate (sRoleID,
                          Updates.set (BSON_ROLE_NAME, sNewName),
                          true,
                          () -> m_aCallbacks.forEach (aCB -> aCB.onRoleRenamed (sRoleID)));
  }

  @Override
  public @NonNull EChange setRoleData (@Nullable final String sRoleID,
                                       @NonNull @Nonempty final String sNewName,
                                       @Nullable final String sNewDescription,
                                       @Nullable final Map <String, String> aNewCustomAttrs)
  {
    final Bson aUpdate = Updates.combine (Updates.set (BSON_ROLE_NAME, sNewName),
                                          Updates.set (BSON_ROLE_DESCRIPTION, sNewDescription),
                                          Updates.set (BSON_ATTRIBUTES, aNewCustomAttrs));

    return genericUpdate (sRoleID, aUpdate, true, () -> m_aCallbacks.forEach (aCB -> aCB.onRoleUpdated (sRoleID)));
  }
}
