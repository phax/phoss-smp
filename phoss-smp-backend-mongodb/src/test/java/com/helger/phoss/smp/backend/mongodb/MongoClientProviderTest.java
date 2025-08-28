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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.bson.Document;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.collection.commons.CommonsArrayList;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

/**
 * Test class for class {@link MongoClientProvider}.
 *
 * @author Philip Helger
 */
public final class MongoClientProviderTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MongoClientProviderTest.class);

  @Test
  public void testBasic ()
  {
    try (final MongoClientProvider aCP = new MongoClientProvider ("mongodb://localhost", "smp-unittest"))
    {
      final Document doc = new Document ().append ("name", "MongoDB")
                                          .append ("type", "database")
                                          .append ("count", Integer.valueOf (1))
                                          .append ("versions", new CommonsArrayList <> ("v3.2", "v3.0", "v2.6"))
                                          .append ("info",
                                                   new Document ().append ("x", Integer.valueOf (203))
                                                                  .append ("y", Integer.valueOf (102)));
      final MongoCollection <Document> aCollection = aCP.getCollection ("coll1");

      final String sIndexName = aCollection.createIndex (new Document ("name", MongoClientProvider.SORT_ASCENDING));
      LOGGER.info ("Index name: " + sIndexName);

      assertNotNull (aCollection);
      assertEquals (0, aCollection.countDocuments ());

      aCollection.insertOne (doc);
      assertEquals (1, aCollection.countDocuments ());
      LOGGER.info (aCollection.countDocuments () + " docs");

      Document aDoc = aCollection.find ().first ();
      assertNotNull (aDoc);
      LOGGER.info ("Created: " + aDoc.toJson ());

      DeleteResult aDR = aCollection.deleteMany (new Document ("name", "MongoDB"));
      LOGGER.info ("DeleteResult = " + aDR);
      assertNotNull (aDR);
      assertTrue (aDR.wasAcknowledged ());
      assertEquals (1, aDR.getDeletedCount ());

      LOGGER.info (aCollection.countDocuments () + " docs");
      assertEquals (0, aCollection.countDocuments ());

      UpdateResult aUR = aCollection.updateOne (new Document ("name", "MongoDB"), Updates.set ("type", "bla"));
      LOGGER.info ("UpdateResult = " + aUR);
      assertNotNull (aUR);
      assertTrue (aUR.wasAcknowledged ());
      assertEquals (0, aUR.getMatchedCount ());
      assertEquals (0, aUR.getModifiedCount ());

      aUR = aCollection.updateMany (new Document ("name", "MongoDB"), Updates.set ("type", "bla"));
      LOGGER.info ("UpdateResult = " + aUR);
      assertNotNull (aUR);
      assertTrue (aUR.wasAcknowledged ());
      assertEquals (0, aUR.getMatchedCount ());
      assertEquals (0, aUR.getModifiedCount ());

      aCollection.insertOne (doc);
      assertEquals (1, aCollection.countDocuments ());
      LOGGER.info (aCollection.countDocuments () + " docs");

      aDoc = aCollection.find ().first ();
      assertNotNull (aDoc);
      LOGGER.info ("Created: " + aDoc.toJson ());

      aUR = aCollection.updateOne (new Document ("name", "MongoDB"), Updates.set ("type", "Update1"));
      LOGGER.info ("UpdateResult = " + aUR);
      assertNotNull (aUR);
      assertTrue (aUR.wasAcknowledged ());
      assertEquals (1, aUR.getMatchedCount ());
      assertEquals (1, aUR.getModifiedCount ());

      aUR = aCollection.updateMany (new Document ("name", "MongoDB"), Updates.set ("type", "Update2"));
      LOGGER.info ("UpdateResult = " + aUR);
      assertNotNull (aUR);
      assertTrue (aUR.wasAcknowledged ());
      assertEquals (1, aUR.getMatchedCount ());
      assertEquals (1, aUR.getModifiedCount ());

      aDR = aCollection.deleteMany (new Document ("name", "MongoDB"));
      LOGGER.info ("DeleteResult = " + aDR);
      assertNotNull (aDR);
      assertTrue (aDR.wasAcknowledged ());
      assertEquals (1, aDR.getDeletedCount ());

      aDR = aCollection.deleteMany (new Document ("name", "MongoDB"));
      LOGGER.info ("DeleteResult = " + aDR);
      assertNotNull (aDR);
      assertTrue (aDR.wasAcknowledged ());
      assertEquals (0, aDR.getDeletedCount ());
    }
  }
}
