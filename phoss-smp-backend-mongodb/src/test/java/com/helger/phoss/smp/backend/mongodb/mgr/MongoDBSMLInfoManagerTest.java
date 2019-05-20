package com.helger.phoss.smp.backend.mongodb.mgr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.helger.commons.collection.impl.ICommonsList;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.photon.app.mock.PhotonAppWebTestRule;

/**
 * Test class for class {@link MongoDBSMLInfoManager}
 *
 * @author Philip Helger
 */
public final class MongoDBSMLInfoManagerTest
{
  @Rule
  public final PhotonAppWebTestRule m_aRule = new PhotonAppWebTestRule ();

  @Test
  public void testBasic ()
  {
    try (final MongoDBSMLInfoManager aMgr = new MongoDBSMLInfoManager ())
    {
      assertEquals (0, aMgr.getAllSMLInfos ().size ());
      final ICommonsList <ISMLInfo> aCreated = aMgr.getAllSMLInfos ();
      for (final ESML e : ESML.values ())
      {
        final ISMLInfo aCreate = aMgr.createSMLInfo (e.getDisplayName (),
                                                     e.getDNSZone (),
                                                     e.getManagementServiceURL (),
                                                     e.isClientCertificateRequired ());
        aCreated.add (aCreate);
      }
      final ICommonsList <ISMLInfo> aAll = aMgr.getAllSMLInfos ();
      assertEquals (ESML.values ().length, aAll.size ());
      for (final ISMLInfo aCreate : aCreated)
        assertTrue (aAll.contains (aCreate));
      for (final ISMLInfo aCreate : aCreated)
        assertTrue (aMgr.updateSMLInfo (aCreate.getID (),
                                        "bla " + aCreate.getDisplayName (),
                                        aCreate.getDNSZone (),
                                        aCreate.getManagementServiceURL (),
                                        aCreate.isClientCertificateRequired ())
                        .isChanged ());
      for (final ISMLInfo aCreate : aCreated)
      {
        final ISMLInfo aInfo = aMgr.getSMLInfoOfID (aCreate.getID ());
        assertNotNull (aInfo);
        assertTrue (aInfo.getDisplayName ().startsWith ("bla "));
      }
      for (final ISMLInfo aCreate : aCreated)
        assertTrue (aMgr.removeSMLInfo (aCreate.getID ()).isChanged ());
      assertEquals (0, aMgr.getAllSMLInfos ().size ());
    }
  }
}
