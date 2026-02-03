package com.helger.phoss.smp.backend.mongodb.security;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.photon.security.object.StubObject;
import com.helger.photon.security.role.IRole;
import com.helger.photon.security.role.IRoleManager;
import com.helger.photon.security.role.IRoleModificationCallback;
import com.helger.photon.security.role.Role;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

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

  @Override
  public void createDefaultsForTest ()
  {
    //ignored for now
  }

  @Override
  public @NonNull CallbackList <IRoleModificationCallback> roleModificationCallbacks ()
  {
    return m_aCallbacks;
  }

  @Override
  public @Nullable IRole createNewRole (@NonNull @Nonempty String sName, @Nullable String sDescription, @Nullable Map <String, String> aCustomAttrs)
  {
    // Create role
    final Role aRole = new Role (sName, sDescription, aCustomAttrs);
    return internalCreateNewRole (aRole, false);
  }

  @Override
  public @Nullable IRole createPredefinedRole (@NonNull @Nonempty String sID, @NonNull @Nonempty String sName, @Nullable String sDescription, @Nullable Map <String, String> aCustomAttrs)
  {
    // Create role
    final Role aRole = new Role (StubObject.createForCurrentUserAndID (sID, aCustomAttrs), sName, sDescription);
    return internalCreateNewRole (aRole, true);
  }

  protected Role internalCreateNewRole (@NonNull final Role aRole, final boolean bPredefined)
  {
    getCollection ().insertOne (toBson (aRole));
    m_aCallbacks.forEach (aCB -> aCB.onRoleCreated (aRole, bPredefined));
    return aRole;
  }

  @Override
  public @NonNull EChange deleteRole (@Nullable String sRoleID)
  {
    return deleteEntity (sRoleID, () -> m_aCallbacks.forEach (aCB -> aCB.onRoleDeleted (sRoleID)));
  }


  @Override
  public @Nullable IRole getRoleOfID (@Nullable String sRoleID)
  {
    return findById (sRoleID);
  }

  @Override
  public @NonNull EChange renameRole (@Nullable String sRoleID, @NonNull @Nonempty String sNewName)
  {
    return genericUpdate (sRoleID, Updates.set (BSON_ROLE_NAME, sNewName), true,
                               () -> m_aCallbacks.forEach (aCB -> aCB.onRoleRenamed (sRoleID)));
  }

  @Override
  public @NonNull EChange setRoleData (@Nullable String sRoleID,
                                       @NonNull @Nonempty String sNewName,
                                       @Nullable String sNewDescription,
                                       @Nullable Map <String, String> aNewCustomAttrs)
  {
    Bson update = Updates.combine (
                               Updates.set (BSON_ROLE_NAME, sNewName),
                               Updates.set (BSON_ROLE_DESCRIPTION, sNewDescription),
                               Updates.set (BSON_ATTRIBUTES, aNewCustomAttrs)
    );

    return genericUpdate (sRoleID, update, true, () -> m_aCallbacks.forEach (aCB -> aCB.onRoleUpdated (sRoleID)));
  }

  @NonNull
  @Override
  @ReturnsMutableCopy
  protected Document toBson (@NonNull final IRole aRole)
  {
    return getDefaultBusinessDocument (aRole)
                               .append (BSON_ROLE_NAME, aRole.getName ())
                               .append (BSON_ROLE_DESCRIPTION, aRole.getDescription ());
  }

  @NonNull
  @ReturnsMutableCopy
  @Override
  protected IRole toEntity (@NonNull Document aDoc)
  {
    return new Role (populateStubObject (aDoc),
                               aDoc.getString (BSON_ROLE_NAME),
                               aDoc.getString (BSON_ROLE_DESCRIPTION)
    );
  }

}
