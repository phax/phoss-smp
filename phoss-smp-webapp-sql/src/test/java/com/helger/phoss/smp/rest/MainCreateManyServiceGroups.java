/**
 * Copyright (C) 2014-2021 Philip Helger and contributors
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

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.ArrayHelper;
import com.helger.commons.concurrent.ExecutorServiceHelper;
import com.helger.commons.http.CHttpHeader;
import com.helger.commons.string.StringHelper;
import com.helger.commons.timing.StopWatch;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.participant.PeppolParticipantIdentifier;
import com.helger.photon.security.CSecurity;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.web.scope.mock.WebScopeTestRule;
import com.helger.xsds.peppol.smp1.ObjectFactory;
import com.helger.xsds.peppol.smp1.ServiceGroupType;
import com.helger.xsds.peppol.smp1.ServiceMetadataReferenceCollectionType;

/**
 * Create many service groups - please make sure the SML connection is not
 * enabled.
 *
 * @author Philip Helger
 */
public final class MainCreateManyServiceGroups
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainCreateManyServiceGroups.class);
  private static final BasicAuthClientCredentials CREDENTIALS = new BasicAuthClientCredentials (CSecurity.USER_ADMINISTRATOR_EMAIL,
                                                                                                CSecurity.USER_ADMINISTRATOR_PASSWORD);

  private static void _testResponseJerseyClient (@Nonnull final Response aResponseMsg,
                                                 @Nonempty final int... aStatusCodes)
  {
    final String sResponse = aResponseMsg.readEntity (String.class);
    if (StringHelper.hasText (sResponse))
      LOGGER.error ("HTTP Response: " + sResponse);
    if (!ArrayHelper.contains (aStatusCodes, aResponseMsg.getStatus ()))
      throw new IllegalStateException (aResponseMsg.getStatus () + " is not in " + Arrays.toString (aStatusCodes));
  }

  public static void main (final String [] args)
  {
    final String sServerBasePath = "http://localhost:90";
    final WebScopeTestRule aRule = new WebScopeTestRule ();
    aRule.before ();
    try
    {
      final ObjectFactory aObjFactory = new ObjectFactory ();
      final StopWatch aSWOverall = StopWatch.createdStarted ();
      final int nStart = 0;
      final int nCount = 100;

      final ExecutorService es = Executors.newFixedThreadPool (2);

      for (int i = nStart; i < nStart + nCount; ++i)
      {
        final int idx = i;
        es.submit ( () -> {
          final StopWatch aSW = StopWatch.createdStarted ();
          final PeppolParticipantIdentifier aPI = PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:test-philip-" +
                                                                                                                                 StringHelper.getLeadingZero (idx,
                                                                                                                                                              7));
          final String sPI = aPI.getURIEncoded ();

          final ServiceGroupType aSG = new ServiceGroupType ();
          aSG.setParticipantIdentifier (aPI);
          aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

          try (final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
          {
            // Delete old - don't care about the result
            if (false)
              ClientBuilder.newClient ()
                           .target (sServerBasePath)
                           .path (sPI)
                           .request ()
                           .header (CHttpHeader.AUTHORIZATION, CREDENTIALS.getRequestValue ())
                           .delete ();

            // Create a new
            final Response aResponseMsg = ClientBuilder.newClient ()
                                                       .target (sServerBasePath)
                                                       .path (sPI)
                                                       .request ()
                                                       .header (CHttpHeader.AUTHORIZATION,
                                                                CREDENTIALS.getRequestValue ())
                                                       .put (Entity.xml (aObjFactory.createServiceGroup (aSG)));
            _testResponseJerseyClient (aResponseMsg, 200);
          }

          aSW.stop ();
          LOGGER.info (sPI + " took " + aSW.getMillis () + " ms");
        });
      }

      ExecutorServiceHelper.shutdownAndWaitUntilAllTasksAreFinished (es);
      aSWOverall.stop ();
      LOGGER.info ("Overall process took " +
                   aSWOverall.getMillis () +
                   " ms or " +
                   aSWOverall.getSeconds () +
                   " seconds");
    }
    finally
    {
      aRule.after ();
    }
  }
}
