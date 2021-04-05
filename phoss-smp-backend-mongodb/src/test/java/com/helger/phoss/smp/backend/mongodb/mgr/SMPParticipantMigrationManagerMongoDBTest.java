/**
 * Copyright (C) 2019-2021 Philip Helger and contributors
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
import static org.junit.Assert.assertNotNull;

import org.bson.Document;
import org.junit.Rule;
import org.junit.Test;

import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.pmigration.SMPParticipantMigration;
import com.helger.phoss.smp.mock.SMPServerTestRule;

/**
 * Test class for class {@link SMPParticipantMigrationManagerMongoDB}.
 *
 * @author Philip Helger
 */
public final class SMPParticipantMigrationManagerMongoDBTest
{
  @Rule
  public final SMPServerTestRule m_aRule = new SMPServerTestRule ();

  @Test
  public void testConversion ()
  {
    final SMPParticipantMigration a = SMPParticipantMigration.createInbound (new SimpleParticipantIdentifier ("iso6523-actorid-upis",
                                                                                                              "9901:test"),
                                                                             "abc123");
    final Document d = SMPParticipantMigrationManagerMongoDB.toBson (a);
    assertNotNull (d);
    final SMPParticipantMigration a2 = SMPParticipantMigrationManagerMongoDB.toDomain (d);
    assertNotNull (a2);
    assertEquals (a, a2);
    assertEquals (d, SMPParticipantMigrationManagerMongoDB.toBson (a2));
  }
}
