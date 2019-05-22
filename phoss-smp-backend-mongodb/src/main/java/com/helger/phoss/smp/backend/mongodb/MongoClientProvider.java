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

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.io.stream.StreamHelper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoClientProvider implements AutoCloseable
{
  public static final Integer INDEX_ASCENDING = Integer.valueOf (1);
  public static final Integer INDEX_DESCENDING = Integer.valueOf (-1);

  private final MongoClient m_aMongoClient;
  private final MongoDatabase m_aDatabase;

  public MongoClientProvider (@Nonnull @Nonempty final String sConnectionString,
                              @Nonnull @Nonempty final String sDBName)
  {
    ValueEnforcer.notEmpty (sConnectionString, "ConnectionString");
    ValueEnforcer.notEmpty (sDBName, "DBName");

    m_aMongoClient = MongoClients.create (sConnectionString);
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
