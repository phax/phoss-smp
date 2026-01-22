/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.array.ArrayHelper;
import com.helger.base.concurrent.ExecutorServiceHelper;
import com.helger.base.string.StringHelper;
import com.helger.base.timing.StopWatch;
import com.helger.http.CHttpHeader;
import com.helger.peppolid.peppol.participant.PeppolParticipantIdentifier;
import com.helger.servlet.mock.MockHttpServletRequest;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.web.scope.mock.WebScopeTestRule;
import com.helger.xsds.peppol.smp1.ObjectFactory;
import com.helger.xsds.peppol.smp1.ServiceGroupType;
import com.helger.xsds.peppol.smp1.ServiceMetadataReferenceCollectionType;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;

/**
 * Create many service groups - please make sure the SML connection is not enabled.
 *
 * @author Philip Helger
 */
public final class MainCreateManyServiceGroups extends AbstractCreateMany
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MainCreateManyServiceGroups.class);

  private static void _testResponseJerseyClient (@NonNull final Response aResponseMsg,
                                                 @Nonempty final int... aStatusCodes)
  {
    final String sResponse = aResponseMsg.readEntity (String.class);
    if (StringHelper.isNotEmpty (sResponse))
      LOGGER.error ("HTTP Response: " + sResponse);
    if (!ArrayHelper.contains (aStatusCodes, aResponseMsg.getStatus ()))
      throw new IllegalStateException (aResponseMsg.getStatus () + " is not in " + Arrays.toString (aStatusCodes));
  }

  public static void main (final String [] args)
  {
    final WebScopeTestRule aRule = new WebScopeTestRule ();
    aRule.before ();
    try
    {
      final ObjectFactory aObjFactory = new ObjectFactory ();
      final StopWatch aSWOverall = StopWatch.createdStarted ();

      final ExecutorService es = Executors.newFixedThreadPool (PARALLEL_ACTIONS);

      for (int i = START_INDEX; i < START_INDEX + PARTICIPANT_COUNT; ++i)
      {
        final int idx = i;
        es.submit ( () -> {
          final StopWatch aSW = StopWatch.createdStarted ();
          final PeppolParticipantIdentifier aPI = createPID (idx);
          final String sPI = aPI.getURIEncoded ();

          final ServiceGroupType aSG = new ServiceGroupType ();
          aSG.setParticipantIdentifier (aPI);
          aSG.setServiceMetadataReferenceCollection (new ServiceMetadataReferenceCollectionType ());

          try (final WebScoped aWS = new WebScoped (new MockHttpServletRequest ()))
          {
            // Delete old - don't care about the result
            if (false)
              ClientBuilder.newClient ()
                           .target (SERVER_BASE_PATH)
                           .path (sPI)
                           .request ()
                           .header (CHttpHeader.AUTHORIZATION, CREDENTIALS.getRequestValue ())
                           .delete ();

            // Create a new
            final Response aResponseMsg = ClientBuilder.newClient ()
                                                       .target (SERVER_BASE_PATH)
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
      LOGGER.info ("Overall process took " + aSWOverall.getMillis () + " ms or " + aSWOverall.getDuration ());
    }
    finally
    {
      aRule.after ();
    }
  }
}
