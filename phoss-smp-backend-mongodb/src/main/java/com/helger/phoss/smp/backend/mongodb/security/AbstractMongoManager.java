package com.helger.phoss.smp.backend.mongodb.security;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.id.IHasID;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.phoss.smp.backend.mongodb.MongoClientSingleton;
import com.helger.photon.io.mgr.IPhotonManager;
import com.helger.photon.security.object.BusinessObjectHelper;
import com.helger.photon.security.object.StubObject;
import com.helger.tenancy.IBusinessObject;
import com.helger.typeconvert.impl.TypeConverter;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

public abstract class AbstractMongoManager <TINT extends IHasID <String>, TIMPL extends TINT> implements
                                           IPhotonManager <TINT>
{
  protected static final String BSON_ID = "id";
  protected static final String BSON_CREATION_TIME = "creationdt";
  protected static final String BSON_CREATION_USER_ID = "creationuserid";
  protected static final String BSON_LAST_MOD_TIME = "lastmoddt";
  protected static final String BSON_LAST_MOD_USER_ID = "lastmoduserid";
  protected static final String BSON_DELETED_TIME = "deletedt";
  protected static final String BSON_DELETED_USER_ID = "deleteuserid";
  protected static final String BSON_ATTRIBUTES = "attrs";

  private final String m_sCollectionName;
  private final MongoCollection <Document> m_aCollection;

  protected AbstractMongoManager (final String sCollectionName)
  {
    ValueEnforcer.notNull (sCollectionName, "CollectionName");
    m_sCollectionName = sCollectionName;
    m_aCollection = MongoClientSingleton.getInstance ()
                                        .getCollection (sCollectionName)
                                        .withWriteConcern (WriteConcern.MAJORITY.withJournal (Boolean.TRUE));
  }

  protected abstract @NonNull Document toBson (@NonNull TINT aPojo);

  protected abstract @NonNull TIMPL toEntity (@NonNull Document aBson);

  @Override
  public @NonNull <T1> ICommonsList <T1> getNone ()
  {
    return new CommonsArrayList <> ();
  }

  /**
   * @return The name of the collection as provided in the constructor. Neither <code>null</code>
   *         nor empty.
   */
  @NonNull
  @Nonempty
  public final String getCollectionName ()
  {
    return m_sCollectionName;
  }

  @NonNull
  protected final MongoCollection <Document> getCollection ()
  {
    return m_aCollection;
  }

  @NonNull
  private Bson _whereId (final String sId)
  {
    return Filters.eq (BSON_ID, sId);
  }

  @NonNull
  protected final Bson addLastModToUpdate (@NonNull final Bson aBson)
  {
    return Updates.combine (aBson,
                            Updates.set (BSON_LAST_MOD_TIME, PDTFactory.getCurrentLocalDateTime ()),
                            Updates.set (BSON_LAST_MOD_USER_ID, BusinessObjectHelper.getUserIDOrFallback ()));
  }

  @NonNull
  protected EChange genericUpdate (@Nullable final String sDocumentID,
                                   @NonNull final Bson aUpdate,
                                   final boolean bUpdateLastModification,
                                   @Nullable final Runnable aUpdateCallback)
  {
    if (StringHelper.isEmpty (sDocumentID))
      return EChange.UNCHANGED;

    final long nMatchCount = getCollection ().updateOne (_whereId (sDocumentID),
                                                         bUpdateLastModification ? addLastModToUpdate (aUpdate)
                                                                                 : aUpdate).getMatchedCount ();

    final EChange eChange = EChange.valueOf (nMatchCount > 0);
    if (eChange.isChanged () && aUpdateCallback != null)
      aUpdateCallback.run ();

    return eChange;
  }

  @NonNull
  public EChange deleteEntity (@Nullable final String sEntityId, @Nullable final Runnable aCallback)
  {
    return genericUpdate (sEntityId,
                          Updates.combine (Updates.set (BSON_DELETED_TIME, PDTFactory.getCurrentLocalDateTime ()),
                                           Updates.set (BSON_DELETED_USER_ID,
                                                        BusinessObjectHelper.getUserIDOrFallback ())),
                          false,
                          aCallback);
  }

  @NonNull
  public EChange undeleteEntity (@Nullable final String sEntityId, @Nullable final Runnable aCallback)
  {
    return genericUpdate (sEntityId,
                          Updates.combine (Updates.set (BSON_DELETED_TIME, null),
                                           Updates.set (BSON_DELETED_USER_ID, null)),
                          false,
                          aCallback);
  }

  protected @Nullable TIMPL findFirst (@NonNull final Bson filter)
  {
    final Document aDocument = getCollection ().find (filter).first ();
    if (aDocument == null)
      return null;

    return toEntity (aDocument);
  }

  public @Nullable TIMPL findByID (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return null;

    return findFirst (_whereId (sID));
  }

  @ReturnsMutableCopy
  protected @NonNull ICommonsList <@NonNull TINT> findAll (@Nullable final Bson aFilter)
  {
    final ICommonsList <TINT> ret = new CommonsArrayList <> ();
    if (aFilter != null)
      getCollection ().find (aFilter).forEach (aDoc -> ret.add (toEntity (aDoc)));
    else
      getCollection ().find ().forEach (aDoc -> ret.add (toEntity (aDoc)));
    return ret;
  }

  @Override
  @ReturnsMutableCopy
  public @NonNull ICommonsList <@NonNull TINT> getAll ()
  {
    // do not filter
    return findAll (null);
  }

  @ReturnsMutableCopy
  protected @NonNull ICommonsList <@NonNull TINT> getAllActive ()
  {
    // get all documents where deleted is null
    return findAll (Filters.eq (BSON_DELETED_TIME, null));
  }

  @ReturnsMutableCopy
  protected @NonNull ICommonsList <@NonNull TINT> getAllDeleted ()
  {
    // get all documents where deleted is not null
    return findAll (Filters.ne (BSON_DELETED_TIME, null));
  }

  @Override
  public boolean containsWithID (@Nullable final String sID)
  {
    if (StringHelper.isEmpty (sID))
      return false;

    return getCollection ().find (_whereId (sID)).first () != null;
  }

  @Override
  public boolean containsAllIDs (@Nullable final Iterable <String> aIDs)
  {
    if (aIDs == null)
      return true;

    final Set <ObjectId> aObjectIds = new CommonsHashSet <> (aIDs, ObjectId::new);

    // uses $in
    final long nFoundDocuments = getCollection ().countDocuments (Filters.in (BSON_ID, aObjectIds));

    return aObjectIds.size () == nFoundDocuments;
  }

  @NonNull
  protected static Document getDefaultBusinessDocument (@NonNull final IBusinessObject aBusinessObject)
  {
    return new Document ().append (BSON_ID, aBusinessObject.getID ())
                          .append (BSON_CREATION_TIME,
                                   TypeConverter.convert (aBusinessObject.getCreationDateTime (), Date.class))
                          .append (BSON_CREATION_USER_ID, aBusinessObject.getCreationUserID ())
                          .append (BSON_LAST_MOD_TIME,
                                   TypeConverter.convert (aBusinessObject.getLastModificationDateTime (), Date.class))
                          .append (BSON_LAST_MOD_USER_ID, aBusinessObject.getLastModificationUserID ())
                          .append (BSON_DELETED_TIME,
                                   TypeConverter.convert (aBusinessObject.getDeletionDateTime (), Date.class))
                          .append (BSON_DELETED_USER_ID, aBusinessObject.getDeletionUserID ())
                          // auto cast to Map<String, String>
                          .append (BSON_ATTRIBUTES, aBusinessObject.attrs ());
  }

  @Nullable
  private static Map <String, String> _readAttrs (final Document aDocument)
  {
    final Document aAttrs = aDocument.get (BSON_ATTRIBUTES, Document.class);
    if (aAttrs == null || aAttrs.isEmpty ())
      return null;

    final Map <String, String> ret = new HashMap <> (aAttrs.size ());
    aAttrs.forEach ( (key, value) -> ret.put (key, String.valueOf (value)));
    return ret;
  }

  @NonNull
  protected static StubObject populateStubObject (@NonNull final Document aDocument)
  {
    return new StubObject (aDocument.getString (BSON_ID),
                           TypeConverter.convert (aDocument.getDate (BSON_CREATION_TIME), LocalDateTime.class),
                           aDocument.getString (BSON_CREATION_USER_ID),
                           TypeConverter.convert (aDocument.getDate (BSON_LAST_MOD_TIME), LocalDateTime.class),
                           aDocument.getString (BSON_LAST_MOD_USER_ID),
                           TypeConverter.convert (aDocument.getDate (BSON_DELETED_TIME), LocalDateTime.class),
                           aDocument.getString (BSON_DELETED_USER_ID),
                           _readAttrs (aDocument));
  }
}
