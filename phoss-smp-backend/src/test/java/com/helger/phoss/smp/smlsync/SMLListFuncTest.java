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
package com.helger.phoss.smp.smlsync;

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.numeric.mutable.MutableInt;
import com.helger.base.state.EContinue;
import com.helger.io.file.FileHelper;
import com.helger.peppol.sml.ESML;
import com.helger.peppol.smlclient.ManageParticipantIdentifierServiceCaller;
import com.helger.peppolid.CIdentifier;

/**
 * Functional test that answers the open questions of
 * <a href="https://github.com/phax/phoss-smp/issues/570">issue #570</a> against the Peppol T-SML:
 * is the <code>List()</code> operation of chapter 3.1.2.7 of the SML specification implemented,
 * what page size does it use and is it authorized for an SMP.<br>
 * This requires a Peppol <b>SMP</b> certificate - an Access Point certificate is not sufficient.
 * With the Access Point certificate <code>test-ap-2025-g3.p12</code> the T-SML answers
 * <code>UnauthorizedFault [ERR-102] Certificate ... does not match or was not found</code>.
 * <p>
 * Result of the run on 2026-09-27 with <code>test-smp-2025-g3.p12</code> (CN=POP000306, OU=PEPPOL
 * TEST SMP) for the SMP ID <code>HELGER-SMP</code>:
 * </p>
 * <ul>
 * <li><code>List()</code> is implemented by the T-SML and is authorized for an SMP certificate</li>
 * <li>17 participants were returned in a single page, with an absent
 * <code>NextPageIdentifier</code>, so the page size limit could not be determined - it is only
 * known to be at least 17</li>
 * <li>Requesting the first page without a <code>NextPageIdentifier</code> element works, which
 * confirms the fix in <code>ManageParticipantIdentifierServiceCaller.list (...)</code></li>
 * <li>The returned identifiers are URI encoded exactly like
 * <code>ISMPServiceGroupManager.getAllSMPServiceGroupIDs ()</code> returns them, for example
 * <code>iso6523-actorid-upis::0225:api-global-france-eb2b</code>, so the reconciliation of issue
 * 570 can diff them as plain strings</li>
 * </ul>
 *
 * @author Philip Helger
 */
@Ignore ("Requires a Peppol SMP certificate and performs a remote call")
public final class SMLListFuncTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (SMLListFuncTest.class);

  /** Overridable, because the key store is not part of this module */
  private static final String KEYSTORE_PATH = System.getProperty ("smltest.keystore.path",
                                                                  "../phoss-smp-webapp-sql/src/main/resources/test-smp-2025-g3.p12");
  private static final char [] KEYSTORE_PASSWORD = "peppol".toCharArray ();
  private static final String SMP_ID = "HELGER-SMP";
  private static final int MAX_PAGES = 50;

  @Test
  public void testListAllPages () throws Exception
  {
    final KeyStore aKeyStore = KeyStore.getInstance ("PKCS12");
    try (final InputStream aIS = FileHelper.getInputStream (new File (KEYSTORE_PATH)))
    {
      if (aIS == null)
        throw new IllegalStateException ("Failed to open the key store '" + KEYSTORE_PATH + "'");
      aKeyStore.load (aIS, KEYSTORE_PASSWORD);
    }

    final KeyManagerFactory aKMF = KeyManagerFactory.getInstance (KeyManagerFactory.getDefaultAlgorithm ());
    aKMF.init (aKeyStore, KEYSTORE_PASSWORD);

    final SSLContext aSSLContext = SSLContext.getInstance ("TLS");
    aSSLContext.init (aKMF.getKeyManagers (), null, null);

    final ManageParticipantIdentifierServiceCaller aCaller = new ManageParticipantIdentifierServiceCaller (ESML.PEPPOL_TEST);
    aCaller.setSSLSocketFactory (aSSLContext.getSocketFactory ());

    LOGGER.info ("Listing the participants of SMP '" +
                 SMP_ID +
                 "' at '" +
                 ESML.PEPPOL_TEST.getManagementServiceURL () +
                 "'");

    final MutableInt aTotal = new MutableInt (0);
    final MutableInt aPages = new MutableInt (0);
    final int nPageCount = aCaller.listAllPages (SMP_ID, aPage -> {
      aPages.inc ();
      final int nPageSize = aPage.getParticipantIdentifierCount ();
      aTotal.inc (nPageSize);
      LOGGER.info ("  Page " +
                   aPages.intValue () +
                   " contains " +
                   nPageSize +
                   " participant(s); next page identifier: " +
                   aPage.getNextPageIdentifier ());
      if (aPages.intValue () == 1 && nPageSize > 0)
        LOGGER.info ("  First participant of the first page: " +
                     CIdentifier.getURIEncoded (aPage.getParticipantIdentifierAtIndex (0)));
      // Keep the probe bounded
      return aPages.intValue () >= MAX_PAGES ? EContinue.BREAK : EContinue.CONTINUE;
    });

    LOGGER.info ("Read " + nPageCount + " page(s) with " + aTotal.intValue () + " participant(s) in total");
  }
}
