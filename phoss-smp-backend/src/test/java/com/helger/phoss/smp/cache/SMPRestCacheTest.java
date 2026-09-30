/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
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
package com.helger.phoss.smp.cache;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import java.nio.charset.StandardCharsets;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.scope.mock.ScopeAwareTestSetup;

/**
 * Test class for class {@link SMPRestCache}. This test only covers the
 * in-memory (Caffeine only) mode, because no Redis server is available in the
 * unit test environment.
 *
 * @author Philip Helger
 */
public final class SMPRestCacheTest
{
  @BeforeClass
  public static void beforeClass ()
  {
    System.setProperty ("smp.rest.cache.enabled", "true");
    ScopeAwareTestSetup.setupScopeTests ();
  }

  @AfterClass
  public static void afterClass ()
  {
    ScopeAwareTestSetup.shutdownScopeTests ();
    System.clearProperty ("smp.rest.cache.enabled");
  }

  @Test
  public void testCacheAndInvalidate ()
  {
    final PeppolIdentifierFactory aIF = PeppolIdentifierFactory.INSTANCE;
    final IParticipantIdentifier aPID = aIF.createParticipantIdentifierWithDefaultScheme ("0088:8590012346576");
    final IDocumentTypeIdentifier aDT = aIF.createDocumentTypeIdentifierWithDefaultScheme ("urn:test::Test##urn:test:1.0::2.1");
    final IDocumentTypeIdentifier aDT2 = aIF.createDocumentTypeIdentifierWithDefaultScheme ("urn:test::Test2##urn:test:1.0::2.1");
    final String sHref = "https://smp.example.org/" + aPID.getURIPercentEncoded ();

    final byte [] aSG = "<ServiceGroup/>".getBytes (StandardCharsets.UTF_8);
    final byte [] aSM = "<SignedServiceMetadata/>".getBytes (StandardCharsets.UTF_8);
    final byte [] aSM2 = "<SignedServiceMetadata2/>".getBytes (StandardCharsets.UTF_8);

    assertNull (SMPRestCache.getServiceGroupPayload (aPID, sHref));
    assertNull (SMPRestCache.getServiceMetadataPayload (aPID, aDT));

    SMPRestCache.setServiceGroupPayload (aPID, sHref, aSG);
    SMPRestCache.setServiceMetadataPayload (aPID, aDT, aSM);
    assertArrayEquals (aSG, SMPRestCache.getServiceGroupPayload (aPID, sHref));
    assertArrayEquals (aSM, SMPRestCache.getServiceMetadataPayload (aPID, aDT));

    // Adding a second document type must not drop the first one
    SMPRestCache.setServiceMetadataPayload (aPID, aDT2, aSM2);
    assertArrayEquals (aSM, SMPRestCache.getServiceMetadataPayload (aPID, aDT));
    assertArrayEquals (aSM2, SMPRestCache.getServiceMetadataPayload (aPID, aDT2));

    // A single callback invocation must drop everything of that participant
    new SMPRestCacheInvalidationCallback ().onSMPServiceGroupDeleted (aPID, false);
    assertNull (SMPRestCache.getServiceGroupPayload (aPID, sHref));
    assertNull (SMPRestCache.getServiceMetadataPayload (aPID, aDT));
    assertNull (SMPRestCache.getServiceMetadataPayload (aPID, aDT2));
  }
}
