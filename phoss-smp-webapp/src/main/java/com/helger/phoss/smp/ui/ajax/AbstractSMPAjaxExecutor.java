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

import com.helger.photon.core.ajax.executor.AbstractAjaxExecutorWithContext;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.annotation.Nonnull;

/**
 * Base class for AJAX executors
 * 
 * @author Philip Helger
 */
public abstract class AbstractSMPAjaxExecutor extends AbstractAjaxExecutorWithContext <LayoutExecutionContext>
{
  @Override
  protected final LayoutExecutionContext createLayoutExecutionContext (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope)
  {
    return LayoutExecutionContext.createForAjaxOrAction (aRequestScope);
  }
}
