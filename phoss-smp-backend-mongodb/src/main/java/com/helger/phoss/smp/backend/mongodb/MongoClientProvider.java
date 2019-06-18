/**
 * Copyright (C) 2014-2019 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.mongodb;

import java.time.LocalDateTime;

import javax.annotation.Nonnull;

import org.bson.BsonType;
import org.bson.Document;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.IterableCodecProvider;
import org.bson.codecs.MapCodecProvider;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.jsr310.Jsr310CodecProvider;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.io.stream.StreamHelper;
import com.mongodb.ConnectionString;
import com.mongodb.DBObjectCodecProvider;
import com.mongodb.DBRefCodecProvider;
import com.mongodb.DocumentToDBRefTransformer;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.codecs.GridFSFileCodecProvider;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;

public class MongoClientProvider implements AutoCloseable
{
  public static final Integer INDEX_ASCENDING = Integer.valueOf (1);
  public static final Integer INDEX_DESCENDING = Integer.valueOf (-1);

  private static final CodecRegistry DEFAULT_CODEC_REGISTRY;
  static
  {
    final ICommonsMap <BsonType, Class <?>> aReplacements = new CommonsHashMap <> ();
    aReplacements.put (BsonType.DATE_TIME, LocalDateTime.class);
    final BsonTypeClassMap aBsonTypeClassMap = new BsonTypeClassMap (aReplacements);

    // Based on 3.10.2 registries
    DEFAULT_CODEC_REGISTRY = CodecRegistries.fromProviders (new CommonsArrayList <> (new ValueCodecProvider (),
                                                                                     new BsonValueCodecProvider (),
                                                                                     new DBRefCodecProvider (),
                                                                                     new DBObjectCodecProvider (aBsonTypeClassMap),
                                                                                     new DocumentCodecProvider (aBsonTypeClassMap,
                                                                                                                new DocumentToDBRefTransformer ()),
                                                                                     new IterableCodecProvider (aBsonTypeClassMap,
                                                                                                                new DocumentToDBRefTransformer ()),
                                                                                     new MapCodecProvider (aBsonTypeClassMap,
                                                                                                           new DocumentToDBRefTransformer ()),
                                                                                     new GeoJsonCodecProvider (),
                                                                                     new GridFSFileCodecProvider (),
                                                                                     new Jsr310CodecProvider ()));
  }

  private final MongoClient m_aMongoClient;
  private final MongoDatabase m_aDatabase;

  public MongoClientProvider (@Nonnull @Nonempty final String sConnectionString,
                              @Nonnull @Nonempty final String sDBName)
  {
    ValueEnforcer.notEmpty (sConnectionString, "ConnectionString");
    ValueEnforcer.notEmpty (sDBName, "DBName");

    final MongoClientSettings aClientSettings = MongoClientSettings.builder ()
                                                                   .applicationName ("phoss SMP")
                                                                   .applyConnectionString (new ConnectionString (sConnectionString))
                                                                   // .codecRegistry
                                                                   // (DEFAULT_CODEC_REGISTRY)
                                                                   .build ();
    m_aMongoClient = MongoClients.create (aClientSettings);
    m_aDatabase = m_aMongoClient.getDatabase (sDBName);
  }

  public void close ()
  {
    StreamHelper.close (m_aMongoClient);
  }

  @Nonnull
  public MongoCollection <Document> getCollection (@Nonnull final String sName)
  {
    return m_aDatabase.getCollection (sName);
  }
}
