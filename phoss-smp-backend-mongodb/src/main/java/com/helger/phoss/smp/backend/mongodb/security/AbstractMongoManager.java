package com.helger.phoss.smp.backend.mongodb.security;

import com.helger.base.id.IHasID;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.backend.mongodb.MongoClientSingleton;
import com.helger.photon.io.mgr.IPhotonManager;
import com.helger.photon.security.object.StubObject;
import com.helger.tenancy.IBusinessObject;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.jspecify.annotations.NonNull;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
    this.m_collection = MongoClientSingleton.getInstance ().getCollection (sCollectionName).withWriteConcern (WriteConcern.MAJORITY.withJournal (true));
  }

  protected MongoCollection <Document> getCollection ()
  {
    return m_collection;
  }

  protected Bson whereId (String sId)
  {
    return Filters.eq (BSON_ID, new ObjectId (sId));
  }

  protected @NonNull EChange genericUpdate (String sDocumentID, Bson update)
  {
    if (StringHelper.isEmpty (sDocumentID))
      return EChange.UNCHANGED;

    UpdateResult updateResult = getCollection ().updateOne (whereId (sDocumentID), update);

    return updateResult.getMatchedCount () == 1 ? EChange.CHANGED : EChange.UNCHANGED;
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


  protected static StubObject populateStubObject (Document raw)
  {
    return new StubObject (
                               raw.get (BSON_ID, String.class),
                               raw.get (BSON_CREATION_TIME, LocalDateTime.class),
                               raw.get (BSON_CREATION_USER_ID, String.class),
                               raw.get (BSON_LAST_MOD_TIME, LocalDateTime.class),
                               raw.get (BSON_LAST_MOD_USER_ID, String.class),
                               raw.get (BSON_DELETED_TIME, LocalDateTime.class),
                               raw.get (BSON_DELETED_USER_ID, String.class),
                               readAttrs (raw)
    );
  }


  private static Map <String, String> readAttrs (Document document)
  {
    Document attrs = document.get (BSON_ATTRIBUTES, Document.class);
    if (attrs == null)
      return null;
    Map <String, String> out = new HashMap <> (attrs.size ());

    attrs.forEach ((key, value) -> {
      out.put (key, String.valueOf (value));
    });

    return out;
  }


  @Override
  public @NonNull <T1> ICommonsList <T1> getNone ()
  {
    return new CommonsArrayList <> ();
  }

}
