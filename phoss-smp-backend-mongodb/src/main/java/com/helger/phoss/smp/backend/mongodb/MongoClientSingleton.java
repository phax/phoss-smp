/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
