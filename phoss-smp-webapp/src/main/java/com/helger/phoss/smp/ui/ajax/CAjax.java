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

import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.http.EHttpMethod;
import com.helger.phoss.smp.CSMPServer;
import com.helger.photon.ajax.GlobalAjaxInvoker;
import com.helger.photon.ajax.IAjaxRegistry;
import com.helger.photon.ajax.decl.AjaxFunctionDeclaration;
import com.helger.photon.ajax.decl.IAjaxFunctionDeclaration;
import com.helger.photon.ajax.executor.IAjaxExecutor;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTables;
import com.helger.photon.uictrls.datatables.ajax.AjaxExecutorDataTablesI18N;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

import jakarta.annotation.Nonnull;

/**
 * This class defines the available ajax functions for the public application.
 *
 * @author Philip Helger
 */
@Immutable
public final class CAjax
{
  public static final Predicate <? super IRequestWebScopeWithoutResponse> FILTER_HTTP_POST = x -> x.getHttpMethod () ==
                                                                                                  EHttpMethod.POST;
  public static final Predicate <? super IRequestWebScopeWithoutResponse> FILTER_IS_USER_LOGGED_IN = x -> LoggedInUserManager.getInstance ()
                                                                                                                             .isUserLoggedInInCurrentSession ();

  public static final IAjaxFunctionDeclaration DATATABLES = AjaxFunctionDeclaration.builder ("dataTables")
                                                                                   .executor (AjaxExecutorDataTables.class)
                                                                                   .build ();
  public static final IAjaxFunctionDeclaration DATATABLES_I18N = AjaxFunctionDeclaration.builder ("datatables-i18n")
                                                                                        .executor (new AjaxExecutorDataTablesI18N (CSMPServer.DEFAULT_LOCALE))
                                                                                        .build ();

  public static final IAjaxFunctionDeclaration FUNCTION_BACKEND_CONNECTION_RESET = AjaxFunctionDeclaration.builder ("backendConnectionReset")
                                                                                                          .executor (AjaxExecutorSecureBackendConnectionReset.class)
                                                                                                          .filter (FILTER_IS_USER_LOGGED_IN)
                                                                                                          .build ();

  private static final Logger LOGGER = LoggerFactory.getLogger (CAjax.class);

  private CAjax ()
  {}

  public static void init (@Nonnull final IAjaxRegistry aAjaxRegistry)
  {
    aAjaxRegistry.registerFunction (DATATABLES);
    aAjaxRegistry.registerFunction (DATATABLES_I18N);
    aAjaxRegistry.registerFunction (FUNCTION_BACKEND_CONNECTION_RESET);
    LOGGER.info ("Successfully registered the Ajax functions");
  }

  @Nonnull
  public static AjaxFunctionDeclaration addAjaxWithLogin (@Nonnull final IAjaxExecutor aExecutor)
  {
    // random name
    final AjaxFunctionDeclaration aFunction = AjaxFunctionDeclaration.builder ()
                                                                     .executor (aExecutor)
                                                                     .filter (CAjax.FILTER_IS_USER_LOGGED_IN)
                                                                     .build ();
    GlobalAjaxInvoker.getInstance ().getRegistry ().registerFunction (aFunction);
    return aFunction;
  }
}
