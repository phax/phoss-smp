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

import com.helger.photon.core.ajax.response.AjaxHtmlResponse;
import com.helger.photon.core.app.context.LayoutExecutionContext;

public abstract class AbstractSMPAjaxExecutorHtml extends AbstractSMPAjaxExecutor
{
  // Change the return type to HTML
  @Override
  @Nonnull
  protected abstract AjaxHtmlResponse mainHandleRequest (@Nonnull final LayoutExecutionContext aLEC) throws Exception;
}
