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
import javax.annotation.concurrent.Immutable;

import com.helger.photon.core.ajax.IAjaxFunctionDeclaration;
import com.helger.photon.core.ajax.IAjaxInvoker;
import com.helger.photon.core.ajax.decl.SecureApplicationAjaxFunctionDeclaration;

/**
 * This class registers the available ajax functions
 *
 * @author Philip Helger
 */
@Immutable
public final class CAjaxSecure
{
  public static final IAjaxFunctionDeclaration FUNCTION_CREATE_BUSINESS_CARD_ENTITY_INPUT = new SecureApplicationAjaxFunctionDeclaration ("createBusinessCardEntityInput",
                                                                                                                                          AjaxExecutorSecureCreateBusinessCardEntityInput.class);
  public static final IAjaxFunctionDeclaration FUNCTION_CREATE_BUSINESS_CARD_IDENTIFIER_INPUT = new SecureApplicationAjaxFunctionDeclaration ("createBusinessCardIdentifierInput",
                                                                                                                                              AjaxExecutorSecureCreateBusinessCardIdentifierInput.class);
  public static final IAjaxFunctionDeclaration FUNCTION_CREATE_BUSINESS_CARD_CONTACT_INPUT = new SecureApplicationAjaxFunctionDeclaration ("createBusinessCardContactInput",
                                                                                                                                           AjaxExecutorSecureCreateBusinessCardContactInput.class);
  public static final IAjaxFunctionDeclaration FUNCTION_EXPORT_ALL_SERVICE_GROUPS = new SecureApplicationAjaxFunctionDeclaration ("exportAllServiceGroups",
                                                                                                                                  AjaxExecutorSecureExportAllServiceGroups.class);

  private CAjaxSecure ()
  {}

  public static void init (@Nonnull final IAjaxInvoker aAjaxInvoker)
  {
    aAjaxInvoker.registerFunction (FUNCTION_CREATE_BUSINESS_CARD_ENTITY_INPUT);
    aAjaxInvoker.registerFunction (FUNCTION_CREATE_BUSINESS_CARD_IDENTIFIER_INPUT);
    aAjaxInvoker.registerFunction (FUNCTION_CREATE_BUSINESS_CARD_CONTACT_INPUT);
    aAjaxInvoker.registerFunction (FUNCTION_EXPORT_ALL_SERVICE_GROUPS);
  }
}
