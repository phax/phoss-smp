/**
 * Copyright (C) 2019 Philip Helger and contributors
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bson.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.commons.type.ObjectType;
import com.helger.phoss.smp.backend.mongodb.MongoClientSingleton;
import com.helger.photon.audit.EAuditActionType;
import com.helger.photon.audit.IAuditor;
import com.mongodb.client.MongoCollection;

/**
 * A special implementation of {@link IAuditor} writing data to a MongoDB
 * collection
 *
 * @author Philip Helger
 */
public class MongoDBAuditor implements IAuditor
{
  /** The default collection name if none is provided */
  public static final String DEFAULT_COLLECTION_NAME = "smp-audit";

  private final MongoCollection <Document> m_aCollection;

  /**
   * Default constructor using {@link #DEFAULT_COLLECTION_NAME} as the
   * collection name.
   */
  public MongoDBAuditor ()
  {
    this (DEFAULT_COLLECTION_NAME);
  }

  /**
   * Constructor
   *
   * @param sCollectionName
   *        Collection name to use. May neither be <code>null</code> nor empty.
   */
  public MongoDBAuditor (@Nonnull @Nonempty final String sCollectionName)
  {
    m_aCollection = MongoClientSingleton.getInstance ().getCollection (sCollectionName);
  }

  public void createAuditItem (@Nonnull final EAuditActionType eActionType,
                               @Nonnull final ESuccess eSuccess,
                               @Nullable final ObjectType aActionObjectType,
                               @Nullable final String sAction,
                               @Nullable final Object... aArgs)
  {
    final Document aDoc = new Document ();
    aDoc.append ("dt", PDTFactory.getCurrentLocalDateTime ());
    aDoc.append ("type", eActionType.getID ());
    aDoc.append ("success", Boolean.valueOf (eSuccess.isSuccess ()));
    if (aActionObjectType != null)
      aDoc.append ("objectType", aActionObjectType.getName ());
    if (StringHelper.hasText (sAction))
      aDoc.append ("action", sAction);
    if (ArrayHelper.isNotEmpty (aArgs))
    {
      final ICommonsList <Document> aDocArgs = new CommonsArrayList <> ();
      for (final Object aArg : aArgs)
      {
        Object aRealArg;
        if (aArg == null)
        {
          aRealArg = null;
        }
        else
        {
          // Manually convert to String
          aRealArg = String.valueOf (aArg);
        }
        aDocArgs.add (new Document ().append ("arg", aRealArg));
      }
      aDoc.append ("args", aDocArgs);
    }

    m_aCollection.insertOne (aDoc);
  }
}
