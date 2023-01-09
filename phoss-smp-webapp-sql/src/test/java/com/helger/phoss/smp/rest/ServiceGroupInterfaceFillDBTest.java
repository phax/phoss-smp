/*
 * Copyright (C) 2014-2023 Philip Helger and contributors
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
package com.helger.phoss.smp.rest;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.string.StringHelper;
import com.helger.commons.timing.StopWatch;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.simple.participant.SimpleParticipantIdentifier;
import com.helger.phoss.smp.mock.MockSMPClient;
import com.helger.phoss.smp.mock.SMPServerRESTTestRule;
import com.helger.smpclient.exception.SMPClientException;
import com.helger.smpclient.peppol.SMPClient;
import com.helger.xsds.peppol.smp1.ServiceGroupType;
import com.helger.xsds.peppol.smp1.ServiceMetadataReferenceCollectionType;

/**
 * Create 2000 service groups
 *
 * @author Philip Helger
 */
@Ignore
public final class ServiceGroupInterfaceFillDBTest extends AbstractSMPWebAppSQLTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ServiceGroupInterfaceFillDBTest.class);

  @Rule
  public final SMPServerRESTTestRule m_aRule = new SMPServerRESTTestRule (PROPERTIES_FILE);

  @Test
  public void testCreateAFewServiceGroups () throws SMPClientException
  {
    final SMPClient aSMPClient = new MockSMPClient ();

    final StopWatch aSW = StopWatch.createdStarted ();
    final int nCount = 2_000;
    for (int i = 0; i < nCount; ++i)
    {
      final IParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme (PID_PREFIX_9999_PHOSS +
                                                                                                                        StringHelper.getLeadingZero (i,
                                                                                                                                                     4));
      final ServiceGroupType aSG = new ServiceGroupType ();
      aSG.setParticipantIdentifier (new SimpleParticipantIdentifier (aPI));
      aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());
      aSMPClient.saveServiceGroup (aSG, CREDENTIALS);
    }
    aSW.stop ();
    LOGGER.info ("Created " +
                 nCount +
                 " ServiceGroups in " +
                 aSW.getDuration () +
                 " (= " +
                 (aSW.getMillis () / nCount) +
                 " ms/entry)");
  }
}
