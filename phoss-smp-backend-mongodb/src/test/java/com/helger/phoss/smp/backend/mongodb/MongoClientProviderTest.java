package com.helger.phoss.smp.backend.mongodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.bson.Document;
import org.junit.Test;

import com.helger.commons.collection.impl.CommonsArrayList;
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

      final String sIndexName = aCollection.createIndex (new Document ("name", MongoClientProvider.INDEX_ASCENDING));
      System.out.println ("Index name: " + sIndexName);

      assertNotNull (aCollection);
      assertEquals (0, aCollection.countDocuments ());

      aCollection.insertOne (doc);
      assertEquals (1, aCollection.countDocuments ());
      System.out.println (aCollection.countDocuments () + " docs");

      Document aDoc = aCollection.find ().first ();
      assertNotNull (aDoc);
      System.out.println ("Created: " + aDoc.toJson ());

      DeleteResult aDR = aCollection.deleteMany (new Document ("name", "MongoDB"));
      System.out.println ("DeleteResult = " + aDR);
      assertNotNull (aDR);
      assertTrue (aDR.wasAcknowledged ());
      assertEquals (1, aDR.getDeletedCount ());

      System.out.println (aCollection.countDocuments () + " docs");
      assertEquals (0, aCollection.countDocuments ());

      UpdateResult aUR = aCollection.updateOne (new Document ("name", "MongoDB"), Updates.set ("type", "bla"));
      System.out.println ("UpdateResult = " + aUR);
      assertNotNull (aUR);
      assertTrue (aUR.wasAcknowledged ());
      assertEquals (0, aUR.getMatchedCount ());
      assertEquals (0, aUR.getModifiedCount ());

      aUR = aCollection.updateMany (new Document ("name", "MongoDB"), Updates.set ("type", "bla"));
      System.out.println ("UpdateResult = " + aUR);
      assertNotNull (aUR);
      assertTrue (aUR.wasAcknowledged ());
      assertEquals (0, aUR.getMatchedCount ());
      assertEquals (0, aUR.getModifiedCount ());

      aCollection.insertOne (doc);
      assertEquals (1, aCollection.countDocuments ());
      System.out.println (aCollection.countDocuments () + " docs");

      aDoc = aCollection.find ().first ();
      assertNotNull (aDoc);
      System.out.println ("Created: " + aDoc.toJson ());

      aUR = aCollection.updateOne (new Document ("name", "MongoDB"), Updates.set ("type", "Update1"));
      System.out.println ("UpdateResult = " + aUR);
      assertNotNull (aUR);
      assertTrue (aUR.wasAcknowledged ());
      assertEquals (1, aUR.getMatchedCount ());
      assertEquals (1, aUR.getModifiedCount ());

      aUR = aCollection.updateMany (new Document ("name", "MongoDB"), Updates.set ("type", "Update2"));
      System.out.println ("UpdateResult = " + aUR);
      assertNotNull (aUR);
      assertTrue (aUR.wasAcknowledged ());
      assertEquals (1, aUR.getMatchedCount ());
      assertEquals (1, aUR.getModifiedCount ());

      aDR = aCollection.deleteMany (new Document ("name", "MongoDB"));
      System.out.println ("DeleteResult = " + aDR);
      assertNotNull (aDR);
      assertTrue (aDR.wasAcknowledged ());
      assertEquals (1, aDR.getDeletedCount ());

      aDR = aCollection.deleteMany (new Document ("name", "MongoDB"));
      System.out.println ("DeleteResult = " + aDR);
      assertNotNull (aDR);
      assertTrue (aDR.wasAcknowledged ());
      assertEquals (0, aDR.getDeletedCount ());
    }
  }
}
