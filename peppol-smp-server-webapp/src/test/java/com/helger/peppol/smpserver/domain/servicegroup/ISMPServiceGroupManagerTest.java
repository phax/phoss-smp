package com.helger.peppol.smpserver.domain.servicegroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
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
    final String sOwnerID = "o1";
    final String sOwnerID2 = "o2";
    final String sExtension = "<ext val='a' />";

    final ISMPServiceGroupManager aSGMgr = MetaManager.getServiceGroupMgr ();
    assertNotNull (aSGMgr);

    // Check empty state
    assertEquals (0, aSGMgr.getSMPServiceGroupCount ());
    assertEquals (0, aSGMgr.getAllSMPServiceGroups ().size ());
    assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI1));
    assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI2));
    assertNull (aSGMgr.getSMPServiceGroupOfID (aPI1));
    assertNull (aSGMgr.getSMPServiceGroupOfID (aPI2));
    assertEquals (0, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID).size ());
    assertEquals (0, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID2).size ());
    assertTrue (aSGMgr.deleteSMPServiceGroup (aPI1).isUnchanged ());
    assertTrue (aSGMgr.deleteSMPServiceGroup (aPI2).isUnchanged ());
    assertTrue (aSGMgr.updateSMPServiceGroup (sSG1, sOwnerID, sExtension).isUnchanged ());
    assertTrue (aSGMgr.updateSMPServiceGroup (sSG2, sOwnerID, sExtension).isUnchanged ());
    assertTrue (aSGMgr.updateSMPServiceGroup (sSG1, sOwnerID2, sExtension).isUnchanged ());
    assertTrue (aSGMgr.updateSMPServiceGroup (sSG2, sOwnerID2, sExtension).isUnchanged ());

    // Register first and check state
    final ISMPServiceGroup aSG1 = aSGMgr.createSMPServiceGroup (sOwnerID, aPI1, sExtension);
    assertNotNull (aSG1);
    assertEquals (aPI1, aSG1.getParticpantIdentifier ());
    assertEquals (sSG1, aSG1.getID ());
    assertEquals (sOwnerID, aSG1.getOwnerID ());
    assertEquals (sExtension, aSG1.getExtension ());

    // Check manager state
    assertEquals (1, aSGMgr.getSMPServiceGroupCount ());
    assertEquals (1, aSGMgr.getAllSMPServiceGroups ().size ());
    assertTrue (aSGMgr.getAllSMPServiceGroups ().contains (aSG1));
    assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI1));
    assertFalse (aSGMgr.containsSMPServiceGroupWithID (aPI2));
    assertSame (aSG1, aSGMgr.getSMPServiceGroupOfID (aPI1));
    assertNull (aSGMgr.getSMPServiceGroupOfID (aPI2));
    assertEquals (1, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID).size ());
    assertTrue (aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID).contains (aSG1));
    assertEquals (0, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID2).size ());

    // change owner
    assertTrue (aSGMgr.updateSMPServiceGroup (sSG1, sOwnerID, sExtension).isUnchanged ());
    assertTrue (aSGMgr.updateSMPServiceGroup (sSG1, sOwnerID2, sExtension).isChanged ());
    assertEquals (0, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID).size ());
    assertEquals (1, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID2).size ());
    assertTrue (aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID2).contains (aSG1));
    assertTrue (aSGMgr.updateSMPServiceGroup (sSG2, sOwnerID, sExtension).isUnchanged ());
    assertTrue (aSGMgr.updateSMPServiceGroup (sSG2, sOwnerID2, sExtension).isUnchanged ());
    assertEquals (sOwnerID2, aSG1.getOwnerID ());

    final ISMPServiceGroup aSG2 = aSGMgr.createSMPServiceGroup (sOwnerID2, aPI2, sExtension);
    assertNotNull (aSG2);
    assertEquals (aPI2, aSG2.getParticpantIdentifier ());
    assertEquals (sSG2, aSG2.getID ());
    assertEquals (sOwnerID2, aSG2.getOwnerID ());
    assertEquals (sExtension, aSG2.getExtension ());

    // Check manager state
    assertEquals (2, aSGMgr.getSMPServiceGroupCount ());
    assertEquals (2, aSGMgr.getAllSMPServiceGroups ().size ());
    assertTrue (aSGMgr.getAllSMPServiceGroups ().contains (aSG1));
    assertTrue (aSGMgr.getAllSMPServiceGroups ().contains (aSG2));
    assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI1));
    assertTrue (aSGMgr.containsSMPServiceGroupWithID (aPI2));
    assertSame (aSG1, aSGMgr.getSMPServiceGroupOfID (aPI1));
    assertSame (aSG2, aSGMgr.getSMPServiceGroupOfID (aPI2));
    assertEquals (0, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID).size ());
    assertEquals (2, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID2).size ());
    assertTrue (aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID2).contains (aSG1));
    assertTrue (aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID2).contains (aSG2));

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
    assertSame (aSG2, aSGMgr.getSMPServiceGroupOfID (aPI2));
    assertEquals (0, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID).size ());
    assertEquals (1, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID2).size ());
    assertTrue (aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID2).contains (aSG2));

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
    assertEquals (0, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID).size ());
    assertEquals (0, aSGMgr.getAllSMPServiceGroupsOfOwner (sOwnerID2).size ());
    assertTrue (aSGMgr.deleteSMPServiceGroup (aPI1).isUnchanged ());
    assertTrue (aSGMgr.deleteSMPServiceGroup (aPI2).isUnchanged ());
    assertTrue (aSGMgr.updateSMPServiceGroup (sSG1, sOwnerID, sExtension).isUnchanged ());
    assertTrue (aSGMgr.updateSMPServiceGroup (sSG2, sOwnerID, sExtension).isUnchanged ());
    assertTrue (aSGMgr.updateSMPServiceGroup (sSG1, sOwnerID2, sExtension).isUnchanged ());
    assertTrue (aSGMgr.updateSMPServiceGroup (sSG2, sOwnerID2, sExtension).isUnchanged ());
  }
}
