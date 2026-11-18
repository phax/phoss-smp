/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.phoss.smp.backend.mongodb.audit.AuditorMongoDB;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.mongodb.client.MongoCollection;

/**
 * Test the read-only mode of the MongoDB backend.
 *
 * @author Philip Helger
 */
public final class SMPReadOnlyMongoDBTest
{
  private static final String COLLECTION_TRANSPORT_PROFILES = "smp-transportprofile";

  private static int _getIndexCount (@NonNull final MongoCollection <Document> aCollection)
  {
    return aCollection.listIndexes ().into (new CommonsArrayList <> ()).size ();
  }

  @Test
  public void testReadOnlyStartupDoesNotWrite ()
  {
    // Start writable first, so that all migrations were executed
    final SMPServerMongoDBTestRule aWritableRule = new SMPServerMongoDBTestRule ();
    aWritableRule.before ();
    aWritableRule.after ();

    try (final MongoClientProvider aCP = new MongoClientProvider (SMPMongoConfiguration.getMongoConnectionString (),
                                                                  SMPMongoConfiguration.getMongoDBName ()))
    {
      // Remove all indexes except the mandatory "_id" index
      final MongoCollection <Document> aTPCollection = aCP.getCollection (COLLECTION_TRANSPORT_PROFILES);
      aTPCollection.dropIndexes ();
      assertEquals (1, _getIndexCount (aTPCollection));

      final MongoCollection <Document> aAuditCollection = aCP.getCollection (AuditorMongoDB.DEFAULT_COLLECTION_NAME);
      final long nAuditCount = aAuditCollection.countDocuments ();

      SMPMetaManager.setReadOnly (true);
      try
      {
        final SMPServerMongoDBTestRule aReadOnlyRule = new SMPServerMongoDBTestRule ();
        aReadOnlyRule.before ();
        try
        {
          assertTrue (MongoClientSingleton.isDBReadable ());
          assertNotNull (SMPMetaManager.getTransportProfileMgr ().getAllSMPTransportProfiles ());
        }
        finally
        {
          aReadOnlyRule.after ();
        }
      }
      finally
      {
        SMPMetaManager.setReadOnly (false);
      }

      // Neither indexes nor audit items were created
      assertEquals (1, _getIndexCount (aTPCollection));
      assertEquals (nAuditCount, aAuditCollection.countDocuments ());
    }
  }
}
