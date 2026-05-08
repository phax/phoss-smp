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
