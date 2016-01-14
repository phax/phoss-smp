/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.domain.servicegroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smpserver.SMPServerTestRule;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;

/**
 * Test class for class {@link ISMPServiceGroupManager}.
 *
 * @author Philip Helger
 */
public final class ISMPServiceGroupManagerFuncTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  @Test
  public void testBasic ()
  {
    final SimpleParticipantIdentifier aPI1 = SimpleParticipantIdentifier.createWithDefaultScheme ("9999:junittest1");
    final SimpleParticipantIdentifier aPI2 = SimpleParticipantIdentifier.createWithDefaultScheme ("9999:junittest2");
    final String sSG1 = SMPServiceGroup.createSMPServiceGroupID (aPI1);
    final String sSG2 = SMPServiceGroup.createSMPServiceGroupID (aPI2);
    final String sOwner1ID = "junitsg1";
    final String sOwner2ID = "junitsg2";
    final String sExtension = "<ext val='a' />";

    final ISMPUserManager aUserMgr = SMPMetaManager.getUserMgr ();
    aUserMgr.createUser (sOwner1ID, "any");
    aUserMgr.createUser (sOwner2ID, "any");
    try
    {
      final ISMPServiceGroupManager aSGMgr = SMPMetaManager.getServiceGroupMgr ();
      assertNotNull (aSGMgr);

      // Check empty state
      final int nCount = aSGMgr.getSMPServiceGroupCount ();
      assertEquals (nCount, aSGMgr.getAllSMPServiceGroups ().size ());
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI1));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI2));
      assertNull (aSGMgr.getSMPServiceGroupOfID (aPI1));
      assertNull (aSGMgr.getSMPServiceGroupOfID (aPI2));
      assertEquals (0, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner1ID).size ());
      assertEquals (0, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner2ID).size ());
      assertTrue (aSGMgr.deleteSMPServiceGroup (aPI1).isUnchanged ());
      assertTrue (aSGMgr.deleteSMPServiceGroup (aPI2).isUnchanged ());
      assertTrue (aSGMgr.updateSMPServiceGroup (sSG1, sOwner1ID, sExtension).isUnchanged ());
      assertTrue (aSGMgr.updateSMPServiceGroup (sSG2, sOwner1ID, sExtension).isUnchanged ());
      assertTrue (aSGMgr.updateSMPServiceGroup (sSG1, sOwner2ID, sExtension).isUnchanged ());
      assertTrue (aSGMgr.updateSMPServiceGroup (sSG2, sOwner2ID, sExtension).isUnchanged ());

      // Register first and check state
      ISMPServiceGroup aSG1 = aSGMgr.createSMPServiceGroup (sOwner1ID, aPI1, sExtension);
      assertNotNull (aSG1);
      assertEquals (aPI1, aSG1.getParticpantIdentifier ());
      assertEquals (sSG1, aSG1.getID ());
      assertEquals (sOwner1ID, aSG1.getOwnerID ());
      assertEquals (sExtension, aSG1.getExtension ());

      // Check manager state
      assertEquals (nCount + 1, aSGMgr.getSMPServiceGroupCount ());
      assertEquals (nCount + 1, aSGMgr.getAllSMPServiceGroups ().size ());
      assertTrue (aSGMgr.getAllSMPServiceGroups ().contains (aSG1));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI1));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI2));
      assertEquals (aSG1, aSGMgr.getSMPServiceGroupOfID (aPI1));
      assertNull (aSGMgr.getSMPServiceGroupOfID (aPI2));
      assertEquals (1, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner1ID).size ());
      assertTrue (aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner1ID).contains (aSG1));
      assertEquals (0, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner2ID).size ());

      // change owner
      assertTrue (aSGMgr.updateSMPServiceGroup (sSG1, sOwner1ID, sExtension).isUnchanged ());
      assertTrue (aSGMgr.updateSMPServiceGroup (sSG1, sOwner2ID, sExtension).isChanged ());
      assertEquals (0, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner1ID).size ());
      assertEquals (1, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner2ID).size ());
      assertTrue (aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner2ID).contains (aSG1));
      assertTrue (aSGMgr.updateSMPServiceGroup (sSG2, sOwner1ID, sExtension).isUnchanged ());
      assertTrue (aSGMgr.updateSMPServiceGroup (sSG2, sOwner2ID, sExtension).isUnchanged ());
      aSG1 = aSGMgr.getSMPServiceGroupOfID (aPI1);
      assertEquals (sOwner2ID, aSG1.getOwnerID ());

      final ISMPServiceGroup aSG2 = aSGMgr.createSMPServiceGroup (sOwner2ID, aPI2, sExtension);
      assertNotNull (aSG2);
      assertEquals (aPI2, aSG2.getParticpantIdentifier ());
      assertEquals (sSG2, aSG2.getID ());
      assertEquals (sOwner2ID, aSG2.getOwnerID ());
      assertEquals (sExtension, aSG2.getExtension ());

      // Check manager state
      assertEquals (nCount + 2, aSGMgr.getSMPServiceGroupCount ());
      assertEquals (nCount + 2, aSGMgr.getAllSMPServiceGroups ().size ());
      assertTrue (aSGMgr.getAllSMPServiceGroups ().contains (aSG1));
      assertTrue (aSGMgr.getAllSMPServiceGroups ().contains (aSG2));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI1));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI2));
      assertEquals (aSG1, aSGMgr.getSMPServiceGroupOfID (aPI1));
      assertEquals (aSG2, aSGMgr.getSMPServiceGroupOfID (aPI2));
      assertEquals (0, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner1ID).size ());
      assertEquals (2, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner2ID).size ());
      assertTrue (aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner2ID).contains (aSG1));
      assertTrue (aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner2ID).contains (aSG2));

      // delete SG1
      assertTrue (aSGMgr.deleteSMPServiceGroup (aPI1).isChanged ());
      assertTrue (aSGMgr.deleteSMPServiceGroup (aPI1).isUnchanged ());

      // Check manager state
      assertEquals (nCount + 1, aSGMgr.getSMPServiceGroupCount ());
      assertEquals (nCount + 1, aSGMgr.getAllSMPServiceGroups ().size ());
      assertTrue (aSGMgr.getAllSMPServiceGroups ().contains (aSG2));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI1));
      assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI2));
      assertNull (aSGMgr.getSMPServiceGroupOfID (aPI1));
      assertEquals (aSG2, aSGMgr.getSMPServiceGroupOfID (aPI2));
      assertEquals (0, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner1ID).size ());
      assertEquals (1, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner2ID).size ());
      assertTrue (aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner2ID).contains (aSG2));

      // Finally delete SG2
      assertTrue (aSGMgr.deleteSMPServiceGroup (aPI2).isChanged ());
      assertTrue (aSGMgr.deleteSMPServiceGroup (aPI2).isUnchanged ());

      // Check empty state
      assertEquals (nCount, aSGMgr.getSMPServiceGroupCount ());
      assertEquals (nCount, aSGMgr.getAllSMPServiceGroups ().size ());
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI1));
      assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI2));
      assertNull (aSGMgr.getSMPServiceGroupOfID (aPI1));
      assertNull (aSGMgr.getSMPServiceGroupOfID (aPI2));
      assertEquals (0, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner1ID).size ());
      assertEquals (0, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwner2ID).size ());
      assertTrue (aSGMgr.deleteSMPServiceGroup (aPI1).isUnchanged ());
      assertTrue (aSGMgr.deleteSMPServiceGroup (aPI2).isUnchanged ());
      assertTrue (aSGMgr.updateSMPServiceGroup (sSG1, sOwner1ID, sExtension).isUnchanged ());
      assertTrue (aSGMgr.updateSMPServiceGroup (sSG2, sOwner1ID, sExtension).isUnchanged ());
      assertTrue (aSGMgr.updateSMPServiceGroup (sSG1, sOwner2ID, sExtension).isUnchanged ());
      assertTrue (aSGMgr.updateSMPServiceGroup (sSG2, sOwner2ID, sExtension).isUnchanged ());
    }
    finally
    {
      aUserMgr.deleteUser (sOwner1ID);
      aUserMgr.deleteUser (sOwner2ID);
    }
  }
}
