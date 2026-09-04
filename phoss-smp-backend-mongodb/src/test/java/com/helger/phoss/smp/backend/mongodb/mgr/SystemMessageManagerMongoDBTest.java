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
package com.helger.phoss.smp.backend.mongodb.mgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;

import org.bson.Document;
import org.junit.Rule;
import org.junit.Test;

import com.helger.phoss.smp.backend.mongodb.SMPServerMongoDBTestRule;
import com.helger.photon.mgrs.sysmsg.ESystemMessageType;
import com.helger.photon.mgrs.sysmsg.ISystemMessageData;
import com.helger.typeconvert.impl.TypeConverter;
import com.mongodb.client.model.Filters;

/**
 * Test reading system message data through the MongoDB backend.
 *
 * @author vinit-thummar
 */
public final class SystemMessageManagerMongoDBTest
{
  @Rule
  public final SMPServerMongoDBTestRule m_aRule = new SMPServerMongoDBTestRule ();

  @Test
  public void testSystemMessageData ()
  {
    final SystemMessageManagerMongoDB aMgr = new SystemMessageManagerMongoDB ();
    final Document aOriginal = aMgr.getCollection ().find ().first ();
    final String sMessage = "System message test " + UUID.randomUUID ();
    Object aDocumentID = null;
    try
    {
      assertTrue (aMgr.setSystemMessage (ESystemMessageType.WARNING, sMessage).isChanged ());
      final Document aStored = aMgr.getCollection ().find ().first ();
      assertNotNull (aStored);
      aDocumentID = aStored.get ("_id");

      final ISystemMessageData aData = aMgr.getSystemMessageData ();
      assertEquals (ESystemMessageType.WARNING, aData.getMessageType ());
      assertEquals (sMessage, aData.getMessage ());
      assertTrue (aData.hasMessage ());
      assertNotNull (aData.getLastUpdateDT ());
      assertEquals (TypeConverter.convert (aStored.getDate ("lastupdate"), LocalDateTime.class),
                    aData.getLastUpdateDT ());

      assertTrue (aMgr.setSystemMessage (ESystemMessageType.DEFAULT, null).isChanged ());
      final ISystemMessageData aCleared = aMgr.getSystemMessageData ();
      assertEquals (ESystemMessageType.DEFAULT, aCleared.getMessageType ());
      assertNull (aCleared.getMessage ());
      assertFalse (aCleared.hasMessage ());
      // Reading a later message must not mutate an earlier snapshot.
      assertEquals (sMessage, aData.getMessage ());
    }
    finally
    {
      // Preserve any system message already present in the test database.
      if (aOriginal != null)
        aMgr.getCollection ().replaceOne (Filters.eq ("_id", aOriginal.get ("_id")), aOriginal);
      else
        if (aDocumentID != null)
          aMgr.getCollection ().deleteOne (Filters.eq ("_id", aDocumentID));
    }
  }
}
