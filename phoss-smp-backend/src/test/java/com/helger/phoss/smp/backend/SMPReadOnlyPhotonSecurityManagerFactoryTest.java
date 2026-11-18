/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.backend;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.photon.audit.IAuditManager;
import com.helger.photon.audit.LoggingAuditor;
import com.helger.photon.security.mgr.PhotonSecurityManager;

/**
 * Test class for {@link SMPReadOnlyPhotonSecurityManagerFactory} and
 * {@link SMPReadOnlyAuditManager}.
 *
 * @author Philip Helger
 */
public final class SMPReadOnlyPhotonSecurityManagerFactoryTest
{
  @Test
  public void testReadOnlyDefault ()
  {
    assertFalse (SMPMetaManager.isReadOnly ());
  }

  @Test
  public void testAuditManager () throws Exception
  {
    final IAuditManager aAuditMgr = new SMPReadOnlyPhotonSecurityManagerFactory (new PhotonSecurityManager.FactoryXML ()).createAuditMgr ();
    assertNotNull (aAuditMgr);
    assertTrue (aAuditMgr instanceof SMPReadOnlyAuditManager);
    assertTrue (aAuditMgr.isInMemory ());
    assertNull (aAuditMgr.getBaseDir ());
    assertTrue (aAuditMgr.getAuditor () instanceof LoggingAuditor);

    // Auditing works, but nothing is persisted
    aAuditMgr.getAuditor ().onExecuteSuccess ("read-only-test");
    assertTrue (aAuditMgr.getLastAuditItems (10).isEmpty ());
    assertNull (aAuditMgr.getEarliestAuditDate ());
    aAuditMgr.stop ();
  }
}
