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
package com.helger.phoss.smp.backend.mongodb;

import javax.annotation.Nonnull;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.string.StringHelper;
import com.helger.scope.IScope;
import com.helger.web.scope.singleton.AbstractGlobalWebSingleton;
import com.mongodb.client.MongoCollection;

public class MongoClientSingleton extends AbstractGlobalWebSingleton
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MongoClientSingleton.class);

  private MongoClientProvider m_aProvider;

  /**
   * @deprecated Only called via reflection
   */
  @Deprecated
  @UsedViaReflection
  public MongoClientSingleton ()
  {}

  @Override
  protected void onAfterInstantiation (@Nonnull final IScope aScope)
  {
    // Standard configuration file
    final String sConnectionString = SMPMongoConfiguration.getMongoConnectionString ();
    if (StringHelper.hasNoText (sConnectionString))
      throw new IllegalStateException ("The MongoDB connection string is missing in the configuration. See property '" +
                                       SMPMongoConfiguration.CONFIG_MONGODB_CONNECTION_STRING +
                                       "'");

    final String sDBName = SMPMongoConfiguration.getMongoDBName ();
    if (StringHelper.hasNoText (sDBName))
      throw new IllegalStateException ("The MongoDB database name is missing in the configuration. See property '" +
                                       SMPMongoConfiguration.CONFIG_MONGODB_DB_NAME +
                                       "'");

    LOGGER.info ("Using Mongo DB database name '" + sDBName + "'");

    m_aProvider = new MongoClientProvider (sConnectionString, sDBName);
  }

  @Override
  protected void onBeforeDestroy (final IScope aScopeToBeDestroyed) throws Exception
  {
    StreamHelper.close (m_aProvider);
  }

  @Nonnull
  public static MongoClientSingleton getInstance ()
  {
    return getGlobalSingleton (MongoClientSingleton.class);
  }

  @Nonnull
  public static final MongoClientProvider getClientProvider ()
  {
    return getInstance ().m_aProvider;
  }

  public static boolean isDBWritable ()
  {
    try
    {
      return getClientProvider ().isDBWritable ();
    }
    catch (final IllegalStateException ex)
    {
      // On shutdown
      return false;
    }
  }

  @Nonnull
  public final MongoCollection <Document> getCollection (@Nonnull @Nonempty final String sCollectionName)
  {
    return m_aProvider.getCollection (sCollectionName);
  }
}
