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

import org.jspecify.annotations.NonNull;

import com.helger.base.string.StringHelper;
import com.helger.http.basicauth.BasicAuthClientCredentials;
import com.helger.peppolid.factory.PeppolIdentifierFactory;
import com.helger.peppolid.peppol.participant.PeppolParticipantIdentifier;
import com.helger.photon.security.CSecurity;

abstract class AbstractCreateMany
{
  protected static final int START_INDEX = 0;
  protected static final int PARTICIPANT_COUNT = 20_000;

  protected static final String SERVER_BASE_PATH = "http://localhost:90";
  protected static final BasicAuthClientCredentials CREDENTIALS = new BasicAuthClientCredentials (CSecurity.USER_ADMINISTRATOR_EMAIL,
                                                                                                  CSecurity.USER_ADMINISTRATOR_PASSWORD);
  protected static final int PARALLEL_ACTIONS = 8;

  @NonNull
  protected static final PeppolParticipantIdentifier createPID (final int nIndex)
  {
    return PeppolIdentifierFactory.INSTANCE.createParticipantIdentifierWithDefaultScheme ("9999:test-philip-" +
                                                                                          StringHelper.getLeadingZero (nIndex,
                                                                                                                       7));
  }
}
