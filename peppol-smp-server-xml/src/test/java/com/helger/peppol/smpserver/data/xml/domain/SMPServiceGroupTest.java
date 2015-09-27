/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.data.xml.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.commons.mock.CommonsTestHelper;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroup;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;
import com.helger.photon.basic.security.CSecurity;

/**
 * Test class for class {@link SMPServiceGroup}.
 *
 * @author Philip Helger
 */
public final class SMPServiceGroupTest
{
  @Rule
  public final TestRule m_aTestRule = new PhotonBasicWebTestRule ();

  @Test
  public void testBasic ()
  {
    final SimpleParticipantIdentifier aPI = SimpleParticipantIdentifier.createWithDefaultScheme ("0088:dummy");
    final SMPServiceGroup aSG = new SMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID, aPI, null);
    assertTrue (StringHelper.hasText (aSG.getID ()));
    assertEquals (CSecurity.USER_ADMINISTRATOR_ID, aSG.getOwnerID ());
    assertEquals (aPI, aSG.getParticpantIdentifier ());
    assertNull (aSG.getExtension ());
    CommonsTestHelper.testMicroTypeConversion (aSG);
  }
}
