/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.ui.ajax;

import javax.annotation.Nonnull;

import com.helger.commons.errorlist.FormErrors;
import com.helger.html.hc.IHCNode;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardContact;
import com.helger.peppol.smpserver.ui.secure.PageSecureBusinessCards;
import com.helger.photon.core.ajax.response.AjaxHtmlResponse;
import com.helger.photon.core.app.context.LayoutExecutionContext;

/**
 * Create a new business entity contact input row
 *
 * @author Philip Helger
 */
public final class AjaxExecutorSecureCreateBusinessCardContactInput extends AbstractSMPAjaxExecutorHtml
{
  public static final String PARAM_ENTITY_ID = "entityid";

  @Override
  @Nonnull
  protected AjaxHtmlResponse mainHandleRequest (@Nonnull final LayoutExecutionContext aLEC) throws Exception
  {
    final String sEntityID = aLEC.getAttributeAsString (PARAM_ENTITY_ID);

    final IHCNode aNode = PageSecureBusinessCards.createContactInputForm (sEntityID,
                                                                          (SMPBusinessCardContact) null,
                                                                          (String) null,
                                                                          new FormErrors ());

    // Build the HTML response
    return AjaxHtmlResponse.createSuccess (aLEC.getRequestScope (), aNode);
  }
}
