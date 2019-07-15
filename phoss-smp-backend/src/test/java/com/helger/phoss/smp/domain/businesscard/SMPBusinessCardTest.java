/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.businesscard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import javax.annotation.Nonnull;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.mock.CommonsTestHelper;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.domain.servicegroup.SMPServiceGroup;
import com.helger.phoss.smp.mock.SMPServerTestRule;
import com.helger.photon.security.CSecurity;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.convert.MicroTypeConverter;
import com.helger.xml.microdom.serialize.MicroWriter;

/**
 * Test class for class {@link SMPBusinessCard}
 *
 * @author Philip Helger
 */
public final class SMPBusinessCardTest
{
  @Rule
  public final TestRule m_aTestRule = new SMPServerTestRule ();

  private static void _testXMLConversion (@Nonnull final SMPBusinessCard aBC)
  {
    // Write to XML
    final IMicroElement e = MicroTypeConverter.convertToMicroElement (aBC, "test");
    assertNotNull (e);

    // Read from XML
    // Special resolver needed here!!
    final SMPBusinessCard aObj2 = SMPBusinessCardMicroTypeConverter.convertToNative (e, x -> aBC.getServiceGroup ());
    assertNotNull (aObj2);

    // Write to XML again
    final IMicroElement e2 = MicroTypeConverter.convertToMicroElement (aObj2, "test");
    assertNotNull (e2);

    // Ensure XML representation is identical
    final String sXML1 = MicroWriter.getNodeAsString (e);
    final String sXML2 = MicroWriter.getNodeAsString (e2);
    CommonsTestHelper._assertEquals ("XML representation must be identical", sXML1, sXML2);

    // Ensure they are equals
    CommonsTestHelper.testDefaultImplementationWithEqualContentObject (aBC, aObj2);
  }

  @Test
  public void testBasic ()
  {
    final IParticipantIdentifier aPI = new SimpleParticipantIdentifier (PeppolIdentifierHelper.DEFAULT_PARTICIPANT_SCHEME,
                                                                        "0088:dummy");

    final SMPServiceGroup aSG = new SMPServiceGroup (CSecurity.USER_ADMINISTRATOR_ID, aPI, null);

    // Test empty
    {
      final SMPBusinessCard aBC = new SMPBusinessCard (aSG, new CommonsArrayList <> ());
      assertSame (aSG, aBC.getServiceGroup ());
      assertEquals (aSG.getID (), aBC.getID ());
      assertEquals (aSG.getID (), aBC.getServiceGroupID ());
      assertNotNull (aBC.getAllEntities ());
      assertTrue (aBC.getAllEntities ().isEmpty ());
      CommonsTestHelper.testDefaultSerialization (aBC);
      _testXMLConversion (aBC);
    }

    // with entities
    {
      final SMPBusinessCardEntity aEntity1 = new SMPBusinessCardEntity ();
      aEntity1.names ().add (new SMPBusinessCardName ("Name 1", "de"));
      aEntity1.names ().add (new SMPBusinessCardName ("Name 2", null));
      aEntity1.setCountryCode ("AT");
      aEntity1.setGeographicalInformation ("here\nthere\r\neverywhere");
      aEntity1.identifiers ().add (new SMPBusinessCardIdentifier ("scheme1", "value1"));
      aEntity1.identifiers ().add (new SMPBusinessCardIdentifier ("scheme 2", "value 2"));
      aEntity1.websiteURIs ().add ("http://www.helger.com");
      aEntity1.websiteURIs ().add ("https://github.com/phax/phoss-smp");
      aEntity1.contacts ().add (new SMPBusinessCardContact ("type 1", "Whoever", "with a phone", "and@an.email.org"));
      aEntity1.contacts ()
              .add (new SMPBusinessCardContact ("type 2", "Whoever else", "without a phone", "but@an.email.org"));
      aEntity1.setAdditionalInformation ("this is line1\nfollowed by line 2\r\nand now eoai");
      aEntity1.setRegistrationDate (PDTFactory.getCurrentLocalDate ());

      final SMPBusinessCardEntity aEntity2 = new SMPBusinessCardEntity ();
      aEntity2.names ().add (new SMPBusinessCardName ("Name 1", "de"));
      aEntity2.names ().add (new SMPBusinessCardName ("Name 2", null));
      aEntity2.setCountryCode ("UK");
      aEntity2.setGeographicalInformation ("here\nthere\r\neverywhere");
      aEntity2.identifiers ().add (new SMPBusinessCardIdentifier ("scheme1", "value1"));
      aEntity2.identifiers ().add (new SMPBusinessCardIdentifier ("scheme 2", "value 2"));
      aEntity2.websiteURIs ().add ("http://www.helger.com");
      aEntity2.websiteURIs ().add ("https://github.com/phax/phoss-smp");
      aEntity2.contacts ().add (new SMPBusinessCardContact ("type 1", "Whoever", "with a phone", "and@an.email.org"));
      aEntity2.contacts ()
              .add (new SMPBusinessCardContact ("type 2", "Whoever else", "without a phone", "but@an.email.org"));
      aEntity2.setAdditionalInformation ("this is line1\nfollowed by line 2\r\nand now eoai");
      aEntity2.setRegistrationDate (PDTFactory.getCurrentLocalDate ());

      final SMPBusinessCardEntity aEntity3 = new SMPBusinessCardEntity ();
      aEntity3.names ().add (new SMPBusinessCardName ("Shorty", null));
      aEntity3.setCountryCode ("US");

      final SMPBusinessCard aBC = new SMPBusinessCard (aSG, new CommonsArrayList <> (aEntity1, aEntity2, aEntity3));
      assertSame (aSG, aBC.getServiceGroup ());
      assertEquals (aSG.getID (), aBC.getID ());
      assertEquals (aSG.getID (), aBC.getServiceGroupID ());
      assertNotNull (aBC.getAllEntities ());
      assertEquals (3, aBC.getAllEntities ().size ());
      CommonsTestHelper.testDefaultSerialization (aBC);
      _testXMLConversion (aBC);
    }
  }
}
