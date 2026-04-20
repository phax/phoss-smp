package com.helger.phoss.smp.backend.mongodb;

import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.base.id.factory.MemoryLongIDFactory;
import com.helger.phoss.smp.backend.mongodb.audit.IDFactoryMongoDB;
import com.helger.phoss.smp.mock.SMPServerTestRule;

/**
 * Special version of {@link SMPServerTestRule} for MongoDB.
 * 
 * @author Philip Helger
 */
public class SMPServerMongoDBTestRule extends SMPServerTestRule
{
  @Override
  public void before ()
  {
    super.before ();

    // Set persistent ID provider: MongoDB based
    GlobalIDFactory.setPersistentLongIDFactory (new IDFactoryMongoDB (0));
    GlobalIDFactory.setPersistentIntIDFactory ( () -> (int) GlobalIDFactory.getNewPersistentLongID ());
  }

  @Override
  public void after ()
  {
    // Reset persistent ID factories before the web scope is destroyed,
    // because IDFactoryMongoDB holds a MongoCollection reference that becomes
    // invalid once MongoClientSingleton is closed during scope destruction.
    GlobalIDFactory.setPersistentLongIDFactory (new MemoryLongIDFactory ());

    super.after ();
  }
}
