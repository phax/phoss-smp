package com.helger.phoss.smp.backend.mongodb.security;

import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.id.IHasID;
import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.base.id.factory.IStringIDFactory;
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
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class AbstractMongoManager <T extends IHasID <String>> implements IPhotonManager <T>
{
  static
  {
    //we have entities that do not use a custom id, so we provide a id factory
    GlobalIDFactory.setPersistentStringIDFactory (new IStringIDFactory ()
    {
      @Override
      public @NonNull String getNewID ()
      {
        return UUID.randomUUID ().toString ();
      }
    });
  }

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
    return Filters.eq (BSON_ID, sId);
  }

  protected @NonNull EChange genericUpdate (@Nullable String sDocumentID, Bson update, boolean setLastMod, @Nullable Runnable updateCallback)
  {
    if (StringHelper.isEmpty (sDocumentID))
      return EChange.UNCHANGED;

    EChange hasChanged = getCollection ().updateOne (whereId (sDocumentID), setLastMod ? addLastModToUpdate (update) : update)
                               .getMatchedCount () > 0 ? EChange.CHANGED : EChange.UNCHANGED;

    if (hasChanged.isChanged () && updateCallback != null)
    {
      updateCallback.run ();
    }

    return hasChanged;
  }

  protected Bson addLastModToUpdate (Bson update)
  {
    return Updates.combine (
                               update,
                               Updates.set (BSON_LAST_MOD_TIME, LocalDateTime.now ()),
                               Updates.set (BSON_LAST_MOD_USER_ID, BusinessObjectHelper.getUserIDOrFallback ())
    );
  }

  public @NonNull EChange deleteEntity (@Nullable String sEntityId, Runnable deleteCallback)
  {
    return genericUpdate (sEntityId, Updates.combine (Updates.set (BSON_DELETED_TIME, LocalDateTime.now ()),
                               Updates.set (BSON_DELETED_USER_ID, BusinessObjectHelper.getUserIDOrFallback ())), false, deleteCallback);
  }

  public @NonNull EChange undeleteEntity (@Nullable String sEntityId, Runnable deleteCallback)
  {
    return genericUpdate (sEntityId, Updates.combine (Updates.set (BSON_DELETED_TIME, null),
                               Updates.set (BSON_DELETED_USER_ID, null)), false, deleteCallback);
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
    return findInternal (new Document ()); //do not filter
  }

  protected @NonNull ICommonsList <T> getAllActive ()
  {
    return findInternal (Filters.eq (BSON_DELETED_TIME, null)); //get all documents where deleted is null
  }

  protected @NonNull ICommonsList <T> getAllDeleted ()
  {
    return findInternal (Filters.ne (BSON_DELETED_TIME, null)); //get all documents where deleted is not null
  }

  protected @NonNull ICommonsList <T> findInternal (@NonNull Bson filter)
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

  @VisibleForTesting
  void deleteAll ()
  {
    getCollection ().drop ();
  }

  protected static Document getDefaultBusinessDocument (IBusinessObject aBusinessObject)
  {
    return new Document ().append (BSON_ID, aBusinessObject.getID ())
                               .append (BSON_CREATION_TIME, convertLocalDateTimeToDate (aBusinessObject.getCreationDateTime ()))
                               .append (BSON_CREATION_USER_ID, aBusinessObject.getCreationUserID ())
                               .append (BSON_LAST_MOD_TIME, convertLocalDateTimeToDate (aBusinessObject.getLastModificationDateTime ()))
                               .append (BSON_LAST_MOD_USER_ID, aBusinessObject.getLastModificationUserID ())
                               .append (BSON_DELETED_TIME, convertLocalDateTimeToDate (aBusinessObject.getDeletionDateTime ()))
                               .append (BSON_DELETED_USER_ID, aBusinessObject.getDeletionUserID ())
                               .append (BSON_ATTRIBUTES, aBusinessObject.attrs ()); //auto cast to Map<String, String>
  }


  protected static StubObject populateStubObject (Document aDocument)
  {
    return new StubObject (
                               aDocument.getString (BSON_ID),
                               convertDatenToLocalDateTime (aDocument.getDate (BSON_CREATION_TIME)),
                               aDocument.getString (BSON_CREATION_USER_ID),
                               convertDatenToLocalDateTime (aDocument.getDate (BSON_LAST_MOD_TIME)),
                               aDocument.getString (BSON_LAST_MOD_USER_ID),
                               convertDatenToLocalDateTime (aDocument.getDate (BSON_DELETED_TIME)),
                               aDocument.getString (BSON_DELETED_USER_ID),
                               readAttrs (aDocument)
    );
  }

  protected static LocalDateTime convertDatenToLocalDateTime (Date aDate)
  {
    if (aDate == null)
    {
      return null;
    }
    return aDate.toInstant ().atZone (ZoneId.systemDefault ()).toLocalDateTime ();
  }

  protected static Date convertLocalDateTimeToDate (LocalDateTime localDateTime)
  {
    if (localDateTime == null)
    {
      return null;
    }
    return Date.from (localDateTime.atZone (ZoneId.systemDefault ()).toInstant ());
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
