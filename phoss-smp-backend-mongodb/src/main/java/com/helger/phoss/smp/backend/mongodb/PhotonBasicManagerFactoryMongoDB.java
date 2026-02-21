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

import org.jspecify.annotations.NonNull;

import com.helger.phoss.smp.backend.mongodb.mgr.LongRunningJobResultManagerMongoDB;
import com.helger.phoss.smp.backend.mongodb.mgr.SystemMessageManagerMongoDB;
import com.helger.phoss.smp.backend.mongodb.mgr.SystemMigrationManagerMongoDB;
import com.helger.photon.mgrs.PhotonBasicManager;
import com.helger.photon.mgrs.longrun.ILongRunningJobResultManager;
import com.helger.photon.mgrs.sysmigration.ISystemMigrationManager;
import com.helger.photon.mgrs.sysmsg.ISystemMessageManager;

/**
 * A MongoDB based implementation of PhotonBasicManager.IFactory.
 *
 * @author Philip Helger
 * @since 8.0.16
 */
public final class PhotonBasicManagerFactoryMongoDB implements PhotonBasicManager.IFactory
{
  @NonNull
  public ISystemMigrationManager createSystemMigrationMgr ()
  {
    return new SystemMigrationManagerMongoDB ();
  }

  @NonNull
  public ISystemMessageManager createSystemMessageMgr ()
  {
    return new SystemMessageManagerMongoDB ();
  }

  @NonNull
  public ILongRunningJobResultManager createLongRunningJobResultMgr ()
  {
    return new LongRunningJobResultManagerMongoDB ();
  }

  /**
   * Install the MongoDB basic manager factory and instantiate the singleton. Must be called before
   * {@link PhotonBasicManager#getInstance()} is called anywhere else.
   */
  public static void install ()
  {
    PhotonBasicManager.setFactory (new PhotonBasicManagerFactoryMongoDB ());
    PhotonBasicManager.getInstance ();
  }
}
