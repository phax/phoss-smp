package com.helger.phoss.smp.backend.mongodb.security;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.id.IHasID;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.backend.mongodb.MongoClientSingleton;
import com.helger.photon.io.mgr.IPhotonManager;
import com.helger.photon.security.object.BusinessObjectHelper;
import com.helger.photon.security.object.StubObject;
import com.helger.tenancy.IBusinessObject;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class AbstractMongoManager <T extends IHasID <String>> implements IPhotonManager <T>
{

  protected static final String BSON_ID = "_id";

  protected static final String BSON_CREATION_TIME = "creationdt";
  protected static final String BSON_CREATION_USER_ID = "creationuserid";
  protected static final String BSON_LAST_MOD_TIME = "lastmoddt";
  protected static final String BSON_LAST_MOD_USER_ID = "lastmoduserid";
  protected static final String BSON_DELETED_TIME = "deletedt";
  protected static final String BSON_DELETED_USER_ID = "deleteuserid";
  protected static final String BSON_ATTRIBUTES = "attrs";

  private final MongoCollection <Document> m_collection;

  protected AbstractMongoManager (String sCollectionName)
  {
    this.m_collection = MongoClientSingleton.getInstance ()
                               .getCollection (sCollectionName)
                               .withWriteConcern (WriteConcern.MAJORITY.withJournal (true));
  }

  protected abstract @NonNull Document toBson (@NonNull T pojo);

  protected abstract @NonNull T toEntity (@NonNull Document document);

  @Override
  public @NonNull <T1> ICommonsList <T1> getNone ()
  {
    return new CommonsArrayList <> ();
  }

  @NonNull
  protected MongoCollection <Document> getCollection ()
  {
    return m_collection;
  }

  @NonNull
  protected Bson whereId (String sId)
  {
    return Filters.eq (BSON_ID, new ObjectId (sId));
  }

  protected @NonNull EChange genericUpdate (@Nullable String sDocumentID, Bson update, boolean setLastMod)
  {
    if (StringHelper.isEmpty (sDocumentID))
      return EChange.UNCHANGED;

    UpdateResult updateResult = getCollection ().updateOne (whereId (sDocumentID), setLastMod ? addLastModToUpdate(update) : update);

    return updateResult.getMatchedCount () == 1 ? EChange.CHANGED : EChange.UNCHANGED;
  }

  protected Bson addLastModToUpdate(Bson update) {
    return Updates.combine (
                               update,
                               Updates.set (BSON_LAST_MOD_TIME, LocalDateTime.now ()),
                               Updates.set (BSON_LAST_MOD_USER_ID, BusinessObjectHelper.getUserIDOrFallback ())
    );
  }

  public @NonNull EChange deleteEntity (@Nullable String sEntityId)
  {
    return genericUpdate (sEntityId, Updates.combine (Updates.set (BSON_DELETED_TIME, LocalDateTime.now ()),
                               Updates.set (BSON_DELETED_USER_ID, BusinessObjectHelper.getUserIDOrFallback ())), false);
  }

  public @NonNull EChange undeleteEntity (@Nullable String sEntityId)
  {
    return genericUpdate (sEntityId, Updates.combine (Updates.set (BSON_DELETED_TIME, null),
                               Updates.set (BSON_DELETED_USER_ID, null)), false);
  }

  public @Nullable T findById (@Nullable String sID)
  {
    if (StringHelper.isEmpty (sID))
      return null;

    Document aDocument = getCollection ().find (whereId (sID)).first ();
    if (aDocument == null)
      return null;

    return toEntity (aDocument);
  }

  @Override
  @ReturnsMutableCopy
  public @NonNull ICommonsList <T> getAll ()
  {
    return findInternal (null); //do not filter
  }

  protected @NonNull ICommonsList <T> getAllActive ()
  {
    return findInternal (Filters.eq (BSON_DELETED_TIME, null)); //get all documents where deleted is null
  }

  protected @NonNull ICommonsList <T> getAllDeleted ()
  {
    return findInternal (Filters.ne (BSON_DELETED_TIME, null)); //get all documents where deleted is not null
  }

  private @NonNull ICommonsList <T> findInternal (@Nullable Bson filter)
  {
    final ICommonsList <T> ret = new CommonsArrayList <> ();
    getCollection ().find (filter).forEach (document -> ret.add (toEntity (document)));
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

  protected static Document getDefaultBusinessDocument (IBusinessObject aBusinessObject)
  {
    return new Document ().append (BSON_CREATION_TIME, aBusinessObject.getCreationDateTime ())
                               .append (BSON_CREATION_USER_ID, aBusinessObject.getCreationUserID ())
                               .append (BSON_LAST_MOD_TIME, aBusinessObject.getLastModificationDateTime ())
                               .append (BSON_LAST_MOD_USER_ID, aBusinessObject.getLastModificationUserID ())
                               .append (BSON_DELETED_TIME, aBusinessObject.getDeletionDateTime ())
                               .append (BSON_DELETED_USER_ID, aBusinessObject.getDeletionUserID ())
                               .append (BSON_ATTRIBUTES, aBusinessObject.attrs ()); //auto cast to Map<String, String>
  }


  protected static StubObject populateStubObject (Document aDocument)
  {
    return new StubObject (
                               aDocument.get (BSON_ID, String.class),
                               aDocument.get (BSON_CREATION_TIME, LocalDateTime.class),
                               aDocument.get (BSON_CREATION_USER_ID, String.class),
                               aDocument.get (BSON_LAST_MOD_TIME, LocalDateTime.class),
                               aDocument.get (BSON_LAST_MOD_USER_ID, String.class),
                               aDocument.get (BSON_DELETED_TIME, LocalDateTime.class),
                               aDocument.get (BSON_DELETED_USER_ID, String.class),
                               readAttrs (aDocument)
    );
  }


  private static Map <String, String> readAttrs (Document aDocument)
  {
    Document attrs = aDocument.get (BSON_ATTRIBUTES, Document.class);
    if (attrs == null || attrs.isEmpty ())
      return null;

    Map <String, String> out = new HashMap <> (attrs.size ());
    attrs.forEach ((key, value) -> out.put (key, String.valueOf (value)));
    return out;
  }

}
