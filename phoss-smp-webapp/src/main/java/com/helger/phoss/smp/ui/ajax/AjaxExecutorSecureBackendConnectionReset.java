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
package com.helger.phoss.smp.ui.ajax;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.state.ETriState;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.core.execcontext.LayoutExecutionContext;

import jakarta.annotation.Nonnull;

/**
 * Mark the "Backend connection established" status as "undefined"
 *
 * @author Philip Helger
 * @since 5.3.1
 */
public final class AjaxExecutorSecureBackendConnectionReset extends AbstractSMPAjaxExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AjaxExecutorSecureBackendConnectionReset.class);

  @Override
  protected void mainHandleRequest (@Nonnull final LayoutExecutionContext aLEC,
                                    @Nonnull final PhotonUnifiedResponse aAjaxResponse) throws Exception
  {
    LOGGER.info ("The Backend Connection Established status is reset to undefined");

    // Trigger callback to communicate back to DBExecutor
    SMPMetaManager.getInstance ().setBackendConnectionState (ETriState.UNDEFINED, true);

    // Build the XML response
    aAjaxResponse.createSeeOther (aLEC.getSelfHref ());
  }
}
