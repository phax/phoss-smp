/**
 * Copyright (C) 2012-2016 winenet GmbH - www.winenet.at
 * All Rights Reserved
 *
 * This file is part of the winenet-Kellerbuch software.
 *
 * Proprietary and confidential.
 *
 * Unauthorized copying of this file, via any medium is
 * strictly prohibited.
 */
package com.helger.peppol.smpserver.ui.ajax;

import javax.annotation.Nonnull;

import com.helger.photon.core.ajax.executor.AbstractAjaxExecutorWithContext;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

public abstract class AbstractSMPAjaxExecutor extends AbstractAjaxExecutorWithContext <LayoutExecutionContext>
{
  @Override
  protected final LayoutExecutionContext createLayoutExecutionContext (@Nonnull final IRequestWebScopeWithoutResponse aRequestScope)
  {
    return LayoutExecutionContext.createForAjaxOrAction (aRequestScope);
  }
}
