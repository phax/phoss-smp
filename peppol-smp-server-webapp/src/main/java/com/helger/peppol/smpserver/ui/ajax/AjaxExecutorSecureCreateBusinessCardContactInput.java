/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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

import com.helger.html.hc.IHCNode;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardContact;
import com.helger.peppol.smpserver.ui.secure.PageSecureBusinessCard;
import com.helger.photon.core.PhotonUnifiedResponse;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.photon.core.form.FormErrorList;

/**
 * Create a new business entity contact input row
 *
 * @author Philip Helger
 */
public final class AjaxExecutorSecureCreateBusinessCardContactInput extends AbstractSMPAjaxExecutor
{
  public static final String PARAM_ENTITY_ID = "entityid";

  @Override
  protected void mainHandleRequest (@Nonnull final LayoutExecutionContext aLEC,
                                    @Nonnull final PhotonUnifiedResponse aAjaxResponse) throws Exception
  {
    final String sEntityID = aLEC.params ().getAsString (PARAM_ENTITY_ID);

    final IHCNode aNode = PageSecureBusinessCard.createContactInputForm (aLEC,
                                                                         sEntityID,
                                                                         (SMPBusinessCardContact) null,
                                                                         (String) null,
                                                                         new FormErrorList ());

    // Build the HTML response
    aAjaxResponse.html (aNode);
  }
}
