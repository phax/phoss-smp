package com.helger.phoss.smp.backend.mongodb.security;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.callback.CallbackList;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.photon.security.object.StubObject;
import com.helger.photon.security.role.IRole;
import com.helger.photon.security.role.IRoleManager;
import com.helger.photon.security.role.IRoleModificationCallback;
import com.helger.photon.security.role.Role;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
    //todo
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
    return internalCreateNewRole (aRole, false, true);
  }

  @Override
  public @Nullable IRole createPredefinedRole (@NonNull @Nonempty String sID, @NonNull @Nonempty String sName, @Nullable String sDescription, @Nullable Map <String, String> aCustomAttrs)
  {
    // Create role
    final Role aRole = new Role (StubObject.createForCurrentUserAndID (sID, aCustomAttrs), sName, sDescription);
    return internalCreateNewRole (aRole, true, true);
  }

  protected Role internalCreateNewRole (@NonNull final Role aRole, final boolean bPredefined, final boolean bRunCallback)
  {

    getCollection ().insertOne (toBson (aRole));

    if (bRunCallback)
    {
      // Execute callback as the very last action
      m_aCallbacks.forEach (aCB -> aCB.onRoleCreated (aRole, bPredefined));
    }

    return aRole;
  }

  @Override
  public @NonNull EChange deleteRole (@Nullable String sRoleID)
  {
    return genericUpdate (sRoleID, Updates.set (BSON_DELETED_TIME, LocalDateTime.now ()));
  }

  @Override
  public @Nullable IRole getRoleOfID (@Nullable String sRoleID)
  {
    if (StringHelper.isEmpty (sRoleID))
      return null;

    Document raw = getCollection ().find (whereId (sRoleID)).first ();
    if (raw == null)
      return null;

    return toRole (raw);
  }


  @Override
  public @NonNull EChange renameRole (@Nullable String sRoleID, @NonNull @Nonempty String sNewName)
  {
    return genericUpdate (sRoleID, Updates.set (BSON_ROLE_NAME, sNewName));
  }

  @Override
  public @NonNull EChange setRoleData (@Nullable String sRoleID, @NonNull @Nonempty String sNewName, @Nullable String sNewDescription, @Nullable Map <String, String> aNewCustomAttrs)
  {
    Bson update = Updates.combine (
                               Updates.set (BSON_ROLE_NAME, sNewName),
                               Updates.set (BSON_ROLE_DESCRIPTION, sNewDescription),
                               Updates.set (BSON_ATTRIBUTES, aNewCustomAttrs)
    );

    return genericUpdate (sRoleID, update);
  }

  @Override
  @ReturnsMutableCopy
  public @NonNull ICommonsList <IRole> getAll ()
  {
    final ICommonsList <IRole> ret = new CommonsArrayList <> ();

    getCollection ().find ().forEach (document -> {
      ret.add (toRole (document));
    });

    return ret;
  }

  @Override
  public boolean containsWithID (@Nullable String sID)
  {
    if (StringHelper.isEmpty (sID))
      return false;

    return getCollection ().countDocuments (whereId (sID)) > 0;
  }

  @Override
  public boolean containsAllIDs (@Nullable Iterable <String> aIDs)
  {
    if (aIDs == null)
      return true;

    Set <ObjectId> aObjectIds = StreamSupport.stream (aIDs.spliterator (), false)
                               .map (ObjectId::new).collect (Collectors.toSet ());

    long countDocuments = getCollection ().countDocuments (Filters.in (BSON_ID, aObjectIds)); //uses $in

    return aObjectIds.size () == countDocuments;
  }

  @NonNull
  @ReturnsMutableCopy
  public static Document toBson (@NonNull final IRole aRole)
  {
    return getDefaultBusinessDocument (aRole)
                               .append (BSON_ROLE_NAME, aRole.getName ())
                               .append (BSON_ROLE_DESCRIPTION, aRole.getDescription ());
  }

  @NonNull
  @ReturnsMutableCopy
  private static IRole toRole (Document raw)
  {
    return new Role (populateStubObject (raw), raw.get (BSON_ROLE_NAME, String.class), raw.get (BSON_ROLE_DESCRIPTION, String.class));
  }


}
