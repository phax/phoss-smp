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
import javax.annotation.concurrent.Immutable;

import com.helger.commons.functional.IPredicate;
import com.helger.peppol.smpserver.app.CApp;
import com.helger.photon.core.ajax.IAjaxInvoker;
import com.helger.photon.core.ajax.decl.AjaxFunctionDeclaration;
import com.helger.photon.core.ajax.decl.IAjaxFunctionDeclaration;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTables;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTablesI18N;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

/**
 * This class defines the available ajax functions for the public application.
 *
 * @author Philip Helger
 */
@Immutable
public final class CAjax
{
  public static final IAjaxFunctionDeclaration DATATABLES = AjaxFunctionDeclaration.builder ("dataTables")
                                                                                   .withExecutor (AjaxExecutorDataTables.class)
                                                                                   .build ();
  public static final IAjaxFunctionDeclaration DATATABLES_I18N = AjaxFunctionDeclaration.builder ("datatables-i18n")
                                                                                        .withExecutor (new AjaxExecutorDataTablesI18N (CApp.DEFAULT_LOCALE))
                                                                                        .build ();
  public static final IAjaxFunctionDeclaration LOGIN = AjaxFunctionDeclaration.builder ("login")
                                                                              .withExecutor (AjaxExecutorPublicLogin.class)
                                                                              .build ();
  private static final IPredicate <? super IRequestWebScopeWithoutResponse> FILTER_LOGIN = x -> LoggedInUserManager.getInstance ()
                                                                                                                   .isUserLoggedInInCurrentSession ();
  public static final IAjaxFunctionDeclaration FUNCTION_CREATE_BUSINESS_CARD_ENTITY_INPUT = AjaxFunctionDeclaration.builder ("createBusinessCardEntityInput")
                                                                                                                   .withExecutor (AjaxExecutorSecureCreateBusinessCardEntityInput.class)
                                                                                                                   .withFilter (FILTER_LOGIN)
                                                                                                                   .build ();
  public static final IAjaxFunctionDeclaration FUNCTION_CREATE_BUSINESS_CARD_IDENTIFIER_INPUT = AjaxFunctionDeclaration.builder ("createBusinessCardIdentifierInput")
                                                                                                                       .withExecutor (AjaxExecutorSecureCreateBusinessCardIdentifierInput.class)
                                                                                                                       .withFilter (FILTER_LOGIN)
                                                                                                                       .build ();
  public static final IAjaxFunctionDeclaration FUNCTION_CREATE_BUSINESS_CARD_CONTACT_INPUT = AjaxFunctionDeclaration.builder ("createBusinessCardContactInput")
                                                                                                                    .withExecutor (AjaxExecutorSecureCreateBusinessCardContactInput.class)
                                                                                                                    .withFilter (FILTER_LOGIN)
                                                                                                                    .build ();
  public static final IAjaxFunctionDeclaration FUNCTION_EXPORT_ALL_SERVICE_GROUPS = AjaxFunctionDeclaration.builder ("exportAllServiceGroups")
                                                                                                           .withExecutor (AjaxExecutorSecureExportAllServiceGroups.class)
                                                                                                           .withFilter (FILTER_LOGIN)
                                                                                                           .build ();

  private CAjax ()
  {}

  public static void init (@Nonnull final IAjaxInvoker aAjaxInvoker)
  {
    aAjaxInvoker.registerFunction (DATATABLES);
    aAjaxInvoker.registerFunction (DATATABLES_I18N);
    aAjaxInvoker.registerFunction (LOGIN);
    aAjaxInvoker.registerFunction (FUNCTION_CREATE_BUSINESS_CARD_ENTITY_INPUT);
    aAjaxInvoker.registerFunction (FUNCTION_CREATE_BUSINESS_CARD_IDENTIFIER_INPUT);
    aAjaxInvoker.registerFunction (FUNCTION_CREATE_BUSINESS_CARD_CONTACT_INPUT);
    aAjaxInvoker.registerFunction (FUNCTION_EXPORT_ALL_SERVICE_GROUPS);
  }
}
