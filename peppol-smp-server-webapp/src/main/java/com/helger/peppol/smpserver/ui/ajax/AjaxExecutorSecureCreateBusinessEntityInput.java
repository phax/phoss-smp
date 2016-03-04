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

import com.helger.commons.errorlist.FormErrors;
import com.helger.html.hc.IHCNode;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardEntity;
import com.helger.peppol.smpserver.ui.secure.PageSecureBusinessCards;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.core.ajax.response.AjaxHtmlResponse;
import com.helger.photon.core.app.context.LayoutExecutionContext;

/**
 * Create a new business entity input row
 *
 * @author Philip Helger
 */
public final class AjaxExecutorSecureCreateBusinessEntityInput extends AbstractSMPAjaxExecutorHtml
{
  @Override
  @Nonnull
  protected AjaxHtmlResponse mainHandleRequest (@Nonnull final LayoutExecutionContext aLEC) throws Exception
  {
    IHCNode aNode = PageSecureBusinessCards.createEntityInputForm (aLEC,
                                                                   (SMPBusinessCardEntity) null,
                                                                   (String) null,
                                                                   new FormErrors ());
    if (aNode == null)
      aNode = new BootstrapErrorBox ().addChild ("Es ist ein interner Fehler aufgetreten. Bitte laden Sie die Seite neu.");

    // Build the HTML response
    return AjaxHtmlResponse.createSuccess (aLEC.getRequestScope (), aNode);
  }
}
