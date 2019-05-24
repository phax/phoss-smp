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

import javax.annotation.Nonnull;

import org.bson.Document;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.scope.IScope;
import com.helger.web.scope.singleton.AbstractGlobalWebSingleton;
import com.mongodb.client.MongoCollection;

public class MongoClientSingleton extends AbstractGlobalWebSingleton
{
  private MongoClientProvider m_aProvider;

  @Deprecated
  @UsedViaReflection
  public MongoClientSingleton ()
  {}

  @Override
  protected void onAfterInstantiation (final IScope aScope)
  {
    // TODO make customizable
    final String sConnectionString = "mongodb://localhost";
    final String sDBName = "phoss-smp";
    m_aProvider = new MongoClientProvider (sConnectionString, sDBName);
  }

  @Nonnull
  public static MongoClientSingleton getInstance ()
  {
    return getGlobalSingleton (MongoClientSingleton.class);
  }

  @Nonnull
  public MongoCollection <Document> getCollection (@Nonnull @Nonempty final String sCollectionName)
  {
    return m_aProvider.getCollection (sCollectionName);
  }
}
