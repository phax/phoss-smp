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
import com.helger.peppol.smpserver.data.ISMPUserManagerSPI;
import com.helger.peppol.smpserver.data.SMPUserManagerFactory;
import com.helger.peppol.smpserver.domain.MetaManager;
import com.helger.peppol.smpserver.domain.SMPHelper;
import com.helger.photon.basic.mock.PhotonBasicWebTestRule;

/**
 * Test class for class {@link ISMPServiceGroupManager}.
 *
 * @author Philip Helger
 */
public final class ISMPServiceGroupManagerTest
{
  @Rule
  public final TestRule m_aTestRule = new PhotonBasicWebTestRule ();

  @Test
  public void testBasic ()
  {
    final SimpleParticipantIdentifier aPI1 = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:a");
    final SimpleParticipantIdentifier aPI2 = SimpleParticipantIdentifier.createWithDefaultScheme ("9915:b");
    final String sSG1 = SMPHelper.createSMPServiceGroupID (aPI1);
    final String sSG2 = SMPHelper.createSMPServiceGroupID (aPI2);
    final String sOwner1ID = "o1";
    final String sOwner2ID = "o2";
    final String sExtension = "<ext val='a' />";

    final ISMPUserManagerSPI aUserMgr = SMPUserManagerFactory.getInstance ();
    aUserMgr.createUser (sOwner1ID, "any");
    aUserMgr.createUser (sOwner2ID, "any");
    try
    {
      final ISMPServiceGroupManager aSGMgr = MetaManager.getServiceGroupMgr ();
      assertNotNull (aSGMgr);

      // Check empty state
      assertEquals (0, aSGMgr.getSMPServiceGroupCount ());
      assertEquals (0, aSGMgr.getAllSMPServiceGroups ().size ());
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
      assertEquals (1, aSGMgr.getSMPServiceGroupCount ());
      assertEquals (1, aSGMgr.getAllSMPServiceGroups ().size ());
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
      assertEquals (2, aSGMgr.getSMPServiceGroupCount ());
      assertEquals (2, aSGMgr.getAllSMPServiceGroups ().size ());
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
      assertEquals (1, aSGMgr.getSMPServiceGroupCount ());
      assertEquals (1, aSGMgr.getAllSMPServiceGroups ().size ());
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
      assertEquals (0, aSGMgr.getSMPServiceGroupCount ());
      assertEquals (0, aSGMgr.getAllSMPServiceGroups ().size ());
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
