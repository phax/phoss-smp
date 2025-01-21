/*
 * Copyright (C) 2019-2025 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.mongodb.audit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.type.ObjectType;
import com.helger.commons.typeconvert.TypeConverter;
import com.helger.phoss.smp.backend.mongodb.MongoClientProvider;
import com.helger.phoss.smp.backend.mongodb.MongoClientSingleton;
import com.helger.photon.audit.AuditItem;
import com.helger.photon.audit.EAuditActionType;
import com.helger.photon.audit.IAuditActionStringProvider;
import com.helger.photon.audit.IAuditItem;
import com.helger.photon.audit.IAuditor;
import com.helger.security.authentication.subject.user.CUserID;
import com.helger.security.authentication.subject.user.ICurrentUserIDProvider;
import com.mongodb.client.MongoCollection;

/**
 * A special implementation of {@link IAuditor} writing data to a MongoDB
 * collection
 *
 * @author Philip Helger
 */
public class AuditorMongoDB implements IAuditor
{
  private static final String BSON_DT = "dt";
  private static final String BSON_USERID = "userid";
  private static final String BSON_TYPE = "type";
  private static final String BSON_SUCCESS = "success";
  private static final String BSON_ACTION = "action";

  private static final Logger LOGGER = LoggerFactory.getLogger (AuditorMongoDB.class);

  /** The default collection name if none is provided */
  public static final String DEFAULT_COLLECTION_NAME = "smp-audit";

  private final MongoCollection <Document> m_aCollection;
  private final ICurrentUserIDProvider m_aCurrentUserIDProvider;

  /**
   * Default constructor using {@link #DEFAULT_COLLECTION_NAME} as the
   * collection name.
   *
   * @param aCurrentUserIDProvider
   *        The current user ID provider. May not be <code>null</code>.
   */
  public AuditorMongoDB (@Nonnull final ICurrentUserIDProvider aCurrentUserIDProvider)
  {
    this (DEFAULT_COLLECTION_NAME, aCurrentUserIDProvider);
  }

  /**
   * Constructor
   *
   * @param sCollectionName
   *        Collection name to use. May neither be <code>null</code> nor empty.
   * @param aCurrentUserIDProvider
   *        The current user ID provider. May not be <code>null</code>.
   */
  public AuditorMongoDB (@Nonnull @Nonempty final String sCollectionName,
                         @Nonnull final ICurrentUserIDProvider aCurrentUserIDProvider)
  {
    ValueEnforcer.notEmpty (sCollectionName, "CollectionName");
    m_aCollection = MongoClientSingleton.getInstance ().getCollection (sCollectionName);
    m_aCurrentUserIDProvider = ValueEnforcer.notNull (aCurrentUserIDProvider, "UserIDProvider");
  }

  @Nonnull
  @ReturnsMutableCopy
  public static Document toBson (@Nonnull final IAuditItem aValue)
  {
    return new Document ().append (BSON_DT, TypeConverter.convert (aValue.getDateTime (), Date.class))
                          .append (BSON_USERID, aValue.getUserID ())
                          .append (BSON_TYPE, aValue.getTypeID ())
                          .append (BSON_SUCCESS, Boolean.valueOf (aValue.isSuccess ()))
                          .append (BSON_ACTION, aValue.getAction ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public static AuditItem toDomain (@Nonnull final Document aDoc)
  {
    final LocalDateTime aDT = TypeConverter.convert (aDoc.getDate (BSON_DT), LocalDateTime.class);

    String sAction = aDoc.getString (BSON_ACTION);
    final List <Document> aArgsList = aDoc.getList ("args", Document.class);
    if (aArgsList != null)
    {
      // Old style
      if (sAction != null)
        sAction = aDoc.getString ("objectType");

      final ICommonsList <Object> aArgValues = new CommonsArrayList <> ();
      for (final Document aArgDoc : aArgsList)
        aArgValues.add (aArgDoc.getString ("arg"));

      // Combine now
      sAction = IAuditActionStringProvider.JSON.apply (sAction, aArgValues.toArray ());
    }

    return new AuditItem (aDT,
                          aDoc.getString (BSON_USERID),
                          EAuditActionType.getFromIDOrNull (aDoc.getString (BSON_TYPE)),
                          ESuccess.valueOf (aDoc.getBoolean (BSON_SUCCESS, false)),
                          sAction);
  }

  public void createAuditItem (@Nonnull final EAuditActionType eActionType,
                               @Nonnull final ESuccess eSuccess,
                               @Nullable final ObjectType aActionObjectType,
                               @Nullable final String sAction,
                               @Nullable final Object... aArgs)
  {
    final String sUserID = StringHelper.getNotEmpty (m_aCurrentUserIDProvider.getCurrentUserID (),
                                                     CUserID.USER_ID_GUEST);
    final String sFullAction = IAuditActionStringProvider.JSON.apply (aActionObjectType != null ? aActionObjectType.getName ()
                                                                                                : sAction,
                                                                      aArgs);
    final IAuditItem aAuditItem = new AuditItem (sUserID, eActionType, eSuccess, sFullAction);

    if (MongoClientSingleton.isDBWritable ())
    {
      if (!m_aCollection.insertOne (toBson (aAuditItem)).wasAcknowledged ())
        throw new IllegalStateException ("Failed to insert into MongoDB Collection");
    }
    else
      LOGGER.warn ("Dropping audit item, because MongoDB is in non-writable state");
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <IAuditItem> getLastAuditItems (@Nonnegative final int nMaxItems)
  {
    ValueEnforcer.isGT0 (nMaxItems, "MaxItems");

    final ICommonsList <IAuditItem> ret = new CommonsArrayList <> ();
    m_aCollection.find ()
                 .sort (new Document (BSON_DT, MongoClientProvider.SORT_DESCENDING))
                 .limit (nMaxItems)
                 .forEach (x -> ret.add (toDomain (x)));
    return ret;
  }

  @Nullable
  public LocalDate getEarliestAuditDate ()
  {
    final Document aDoc = m_aCollection.find ()
                                       .sort (new Document (BSON_DT, MongoClientProvider.SORT_ASCENDING))
                                       .batchSize (1)
                                       .first ();
    if (aDoc != null)
    {
      final LocalDateTime aLDT = TypeConverter.convert (aDoc.getDate (BSON_DT), LocalDateTime.class);
      if (aLDT != null)
        return aLDT.toLocalDate ();
    }
    return null;
  }
}
